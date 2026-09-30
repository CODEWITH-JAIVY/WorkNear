output "eks_cluster_name" {
  value = module.eks.cluster_name
}

output "configure_kubectl" {
  value = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}

output "rds_endpoint" {
  value     = aws_db_instance.main.address
  sensitive = true
}

output "redis_endpoint" {
  value = aws_elasticache_cluster.main.cache_nodes[0].address
}

output "ecr_repository_urls" {
  value = { for name, repo in aws_ecr_repository.services : name => repo.repository_url }
}

output "media_cloudfront_domain" {
  value = aws_cloudfront_distribution.media.domain_name
}

output "route53_nameservers" {
  value = var.domain_name != "" ? aws_route53_zone.main[0].name_servers : []
  description = "Point your domain registrar at these — required before ACM validates"
}
