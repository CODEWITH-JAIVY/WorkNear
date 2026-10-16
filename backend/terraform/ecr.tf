# One ECR repo per deployable service — matches the CI/CD path-filter list in .github/workflows/ci-cd.yml
locals {
  service_names = [
    "discovery-server", "config-server", "api-gateway",
    "auth-service", "customer-service", "labour-service", "job-service",
    "matching-service", "notification-service", "media-service",
    "payment-service", "rating-service", "admin-service", "kyc-service", "chat-service"
  ]
}

resource "aws_ecr_repository" "services" {
  for_each             = toset(local.service_names)
  name                 = "${var.project_name}/${each.value}"
  image_tag_mutability = "IMMUTABLE" # every deploy is a new tag (commit SHA) — never overwrite :latest silently

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = { Project = var.project_name }
}

# Keep only the last 20 images per service — ECR storage cost adds up fast with IMMUTABLE tags
resource "aws_ecr_lifecycle_policy" "services" {
  for_each   = aws_ecr_repository.services
  repository = each.value.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 20 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 20
      }
      action = { type = "expire" }
    }]
  })
}
