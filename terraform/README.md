# Terraform — AWS infrastructure for Labourse Searching

Provisions: VPC (2 AZ), EKS (t3.medium x3), RDS MySQL, ElastiCache Redis, 15 ECR repos,
S3+CloudFront for media, Secrets Manager, Route53+ACM (optional), and a GitHub Actions OIDC
deploy role. Kafka is deployed separately via Strimzi on the cluster (see `../k8s/kafka/`) —
not Terraform-managed, see that folder's README for why.

## One-time bootstrap (before the first `terraform apply`)

Terraform's own state needs a home before it can create anything else — chicken-and-egg, so
this part is manual, once, ever:

```bash
aws s3api create-bucket --bucket labourse-terraform-state --region ap-south-1 \
  --create-bucket-configuration LocationConstraint=ap-south-1
aws s3api put-bucket-versioning --bucket labourse-terraform-state \
  --versioning-configuration Status=Enabled
aws dynamodb create-table --table-name labourse-terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST
```

Then update the bucket name in `versions.tf`'s `backend "s3"` block if you named it differently.

## Apply

```bash
cp terraform.tfvars.example terraform.tfvars   # fill in real secrets — this file is gitignored
terraform init
terraform plan      # READ this output before the next line — it lists every resource about to exist
terraform apply
```

Takes ~15-20 minutes, mostly waiting on EKS. When it finishes:

```bash
$(terraform output -raw configure_kubectl)      # wires up your local kubectl
kubectl get nodes                                # should show 3 nodes, Ready
```

## After `terraform apply` — what's still manual

1. **Kafka**: `kubectl apply` the Strimzi manifests in `../k8s/kafka/` (see that README)
2. **Secrets sync**: install External Secrets Operator, apply `../k8s/external-secrets/` (see that README)
3. **Ingress**: install the AWS Load Balancer Controller, apply `../k8s/ingress/` (see that README)
4. **GitHub Actions**: copy `terraform output github_actions_deploy_role_arn` into your repo's
   `AWS_GHA_DEPLOY_ROLE_ARN` secret (Settings → Secrets and variables → Actions)
5. **Your services**: `kubectl apply -f ../k8s/<service>/deployment.yaml` for all 13 deployable
   services (discovery/config/gateway first, then the rest — same order as `docker-compose`'s
   `depends_on` chain)
6. **DNS**: if you set `domain_name`, point your registrar at `terraform output route53_nameservers`;
   ACM won't validate until that propagates (can take a few hours)
7. **mysql-init**: RDS doesn't run `docker-entrypoint-initdb.d` scripts — connect once with a MySQL
   client and run `../mysql-init/01-create-databases.sql` by hand against the RDS endpoint

## Cost reality check (ap-south-1, rough monthly, Sep 2026 pricing — verify current pricing before committing)

| Resource | Approx. |
|---|---|
| EKS control plane | $73 (flat, regardless of node count) |
| 3x t3.medium nodes | ~$90 |
| RDS db.t3.micro | Free Tier first 12 months, then ~$15 |
| ElastiCache cache.t3.micro | ~$12 |
| NAT Gateway | ~$35 + data transfer |
| ALB | ~$20 + traffic |
| **Total, roughly** | **~$150-250/month** even at near-zero traffic |

This is real money for a pre-revenue startup. `terraform destroy` tears everything down when
not actively testing — cheaper to rebuild from this repo each time than to pay for an idle
cluster between work sessions.
