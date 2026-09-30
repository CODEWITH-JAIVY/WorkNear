variable "aws_region" {
  description = "Mumbai region — lowest latency for Delhi NCR users"
  type        = string
  default     = "ap-south-1"
}

variable "project_name" {
  type    = string
  default = "labourse"
}

variable "environment" {
  type    = string
  default = "production"
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "eks_cluster_version" {
  type    = string
  default = "1.30"
}

# Start small — t3.medium x2 is enough for all 15 services at near-zero traffic.
# Scale via node group desired_size, not by changing instance type first.
variable "node_instance_type" {
  type    = string
  default = "t3.medium"
}

variable "node_desired_size" {
  type    = number
  default = 3
}

variable "db_instance_class" {
  description = "db.t3.micro is Free Tier eligible for the first 12 months on a new AWS account"
  type        = string
  default     = "db.t3.micro"
}

variable "domain_name" {
  description = "Your registered domain, e.g. labourse.in — leave blank to skip Route53/ACM"
  type        = string
  default     = ""
}

variable "db_password" {
  description = "Set via terraform.tfvars (gitignored) or TF_VAR_db_password env var — never commit this"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  type      = string
  sensitive = true
}

variable "internal_service_secret" {
  type      = string
  sensitive = true
}

variable "razorpay_key_id" {
  type      = string
  sensitive = true
  default   = ""
}

variable "razorpay_key_secret" {
  type      = string
  sensitive = true
  default   = ""
}

variable "razorpay_webhook_secret" {
  type      = string
  sensitive = true
  default   = ""
}

variable "google_client_id" {
  type    = string
  default = ""
}

variable "google_client_secret" {
  type      = string
  sensitive = true
  default   = ""
}
