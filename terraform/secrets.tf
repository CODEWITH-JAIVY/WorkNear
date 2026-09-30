# All the secrets that today live in .env get created here instead, so `terraform apply` is the
# one place that ever sees them — not scattered across a laptop's .env file and GitHub secrets.
resource "aws_secretsmanager_secret" "app_secrets" {
  name = "${var.project_name}/${var.environment}/app-secrets"
}

resource "aws_secretsmanager_secret_version" "app_secrets" {
  secret_id = aws_secretsmanager_secret.app_secrets.id
  secret_string = jsonencode({
    DB_USERNAME             = "labourse_admin"
    DB_PASSWORD             = var.db_password
    DB_HOST                 = aws_db_instance.main.address
    REDIS_HOST              = aws_elasticache_cluster.main.cache_nodes[0].address
    JWT_SECRET               = var.jwt_secret
    INTERNAL_SERVICE_SECRET  = var.internal_service_secret
    RAZORPAY_KEY_ID          = var.razorpay_key_id
    RAZORPAY_KEY_SECRET      = var.razorpay_key_secret
    RAZORPAY_WEBHOOK_SECRET  = var.razorpay_webhook_secret
    GOOGLE_CLIENT_ID         = var.google_client_id
    GOOGLE_CLIENT_SECRET     = var.google_client_secret
    AWS_S3_BUCKET            = aws_s3_bucket.media.bucket
  })
}
