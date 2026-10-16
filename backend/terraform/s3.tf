resource "aws_s3_bucket" "media" {
  bucket = "${var.project_name}-media-${var.environment}"
  tags   = { Project = var.project_name }
}

resource "aws_s3_bucket_public_access_block" "media" {
  bucket = aws_s3_bucket.media.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Files are served via CloudFront (below), never directly from S3 — the bucket itself stays private.
resource "aws_cloudfront_origin_access_control" "media" {
  name                              = "${var.project_name}-media-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "media" {
  enabled = true

  origin {
    domain_name              = aws_s3_bucket.media.bucket_regional_domain_name
    origin_id                = "media-s3-origin"
    origin_access_control_id = aws_cloudfront_origin_access_control.media.id
  }

  default_cache_behavior {
    target_origin_id       = "media-s3-origin"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods          = ["GET", "HEAD"]
    forwarded_values {
      query_string = false
      cookies { forward = "none" }
    }
  }

  restrictions {
    geo_restriction { restriction_type = "none" }
  }

  viewer_certificate {
    cloudfront_default_certificate = true # swap for an ACM cert once domain_name is set
  }
}

resource "aws_s3_bucket_policy" "media_cloudfront_only" {
  bucket = aws_s3_bucket.media.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "cloudfront.amazonaws.com" }
      Action    = "s3:GetObject"
      Resource  = "${aws_s3_bucket.media.arn}/*"
      Condition = {
        StringEquals = { "AWS:SourceArn" = aws_cloudfront_distribution.media.arn }
      }
    }]
  })
}

# Retention for job attachments (customer photos / video / voice notes), the "within 2 days" rule.
# The "same day once the work is completed" half is NOT here — that's media-service reacting to the
# job.completed event and deleting the keys immediately (media-service JobCompletedListener).
# This rule is the backstop for jobs that never reach COMPLETED (cancelled, abandoned, expired).
#
# Honest caveat: S3 lifecycle works in whole days and runs once a day, so an object with
# days = 2 is removed at the first daily run after it turns 2 days old — in practice somewhere
# between 2 and 3 days after upload, not at exactly 48 hours.
resource "aws_s3_bucket_lifecycle_configuration" "media" {
  bucket = aws_s3_bucket.media.id

  rule {
    id     = "expire-job-attachments"
    status = "Enabled"

    filter {
      prefix = "job-attachments/"
    }

    expiration {
      days = 2
    }
  }
}
