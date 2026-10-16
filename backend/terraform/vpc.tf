# Standard 2-AZ setup: private subnets for EKS nodes/RDS/Redis/Kafka, public subnets only for
# the load balancer. Nothing except the LB should ever get a public IP.
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.8"

  name = "${var.project_name}-vpc"
  cidr = var.vpc_cidr

  azs             = ["${var.aws_region}a", "${var.aws_region}b"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24"]

  enable_nat_gateway   = true
  single_nat_gateway   = true # one NAT for cost — go per-AZ (false) once traffic justifies it
  enable_dns_hostnames = true

  # Required tags for the EKS/ALB controllers to auto-discover these subnets
  public_subnet_tags = {
    "kubernetes.io/role/elb"                          = "1"
    "kubernetes.io/cluster/${var.project_name}-eks"   = "shared"
  }
  private_subnet_tags = {
    "kubernetes.io/role/internal-elb"                 = "1"
    "kubernetes.io/cluster/${var.project_name}-eks"   = "shared"
  }

  tags = { Project = var.project_name, Environment = var.environment }
}
