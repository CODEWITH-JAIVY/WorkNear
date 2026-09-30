# Syncing AWS Secrets Manager → Kubernetes (External Secrets Operator)

Terraform's `secrets.tf` writes everything into one AWS Secrets Manager secret. This operator
pulls it into a k8s Secret every service's `envFrom: secretRef: { name: labourse-app-secrets }`
already points at — so `terraform apply` is the only place a real secret value is ever typed,
never a `kubectl create secret` command in your shell history or a `.env` file on a laptop.

```bash
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets -n external-secrets --create-namespace

kubectl apply -f cluster-secret-store.yaml
kubectl apply -f external-secret.yaml
```

Needs an IAM role (IRSA) with `secretsmanager:GetSecretValue` on the one secret — add this to
`terraform/eks.tf` alongside the existing `media_service_s3` role once you're at this step
(same `aws_iam_role` + `aws_iam_role_policy` pattern, scoped to `aws_secretsmanager_secret.app_secrets.arn`).
