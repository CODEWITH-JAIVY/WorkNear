module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.24"

  cluster_name    = "${var.project_name}-eks"
  cluster_version = var.eks_cluster_version

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  cluster_endpoint_public_access = true # restrict to your office/VPN CIDR once you have one

  eks_managed_node_groups = {
    general = {
      instance_types = [var.node_instance_type]
      min_size       = 2
      max_size       = 6
      desired_size   = var.node_desired_size
    }
  }

  # IRSA (IAM Roles for Service Accounts) — lets media-service's pod assume an IAM role scoped
  # to only the S3 bucket, instead of long-lived AWS access keys baked into env vars.
  enable_irsa = true

  tags = { Project = var.project_name, Environment = var.environment }
}

# media-service's pod uses this role via its k8s ServiceAccount annotation — no AWS keys in env.
resource "aws_iam_role" "media_service_s3" {
  name = "${var.project_name}-media-service-s3"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = module.eks.oidc_provider_arn
      }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${replace(module.eks.oidc_provider, "https://", "")}:sub" = "system:serviceaccount:default:media-service"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "media_service_s3_policy" {
  name = "${var.project_name}-media-s3-policy"
  role = aws_iam_role.media_service_s3.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"]
      Resource = "${aws_s3_bucket.media.arn}/*"
    }]
  })
}
