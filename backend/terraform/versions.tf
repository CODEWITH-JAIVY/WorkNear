terraform {
  required_version = ">= 1.7"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.60"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.31"
    }
  }

  # State should NOT stay local for a real startup — one `terraform apply` from a laptop that
  # dies mid-run corrupts local state with no recovery. Create this bucket + table once by hand
  # (or via a bootstrap/ sub-config) before pointing this block at them.
  backend "s3" {
    bucket         = "labourse-terraform-state"   # replace with your actual bucket name
    key            = "labourse/terraform.tfstate"
    region         = "ap-south-1"
    dynamodb_table = "labourse-terraform-locks"
    encrypt        = true
  }
}

provider "aws" {
  region = var.aws_region
}
