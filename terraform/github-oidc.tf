# Lets GitHub Actions assume an AWS role via OIDC — no long-lived AWS access keys stored as
# GitHub secrets (those leak far more often than anyone likes to admit).
resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

resource "aws_iam_role" "github_actions_deploy" {
  name = "${var.project_name}-github-actions-deploy"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = { Federated = aws_iam_openid_connect_provider.github.arn }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = { "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com" }
        # Restrict to your actual repo — replace OWNER/REPO before applying
        StringLike = { "token.actions.githubusercontent.com:sub" = "repo:OWNER/REPO:ref:refs/heads/main" }
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "github_ecr" {
  role       = aws_iam_role.github_actions_deploy.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPowerUser"
}

# Lets the deploy role update kubectl on the EKS cluster — EKS access entries (not the old
# aws-auth ConfigMap) are the modern way to grant this.
resource "aws_eks_access_entry" "github_actions" {
  cluster_name  = module.eks.cluster_name
  principal_arn = aws_iam_role.github_actions_deploy.arn
}

resource "aws_eks_access_policy_association" "github_actions" {
  cluster_name  = module.eks.cluster_name
  principal_arn = aws_iam_role.github_actions_deploy.arn
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSEditPolicy"

  access_scope {
    type = "cluster"
  }
}

output "github_actions_deploy_role_arn" {
  value = aws_iam_role.github_actions_deploy.arn
  description = "Paste this into the GitHub repo secret AWS_GHA_DEPLOY_ROLE_ARN"
}
