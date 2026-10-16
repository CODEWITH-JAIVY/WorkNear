# ALB Ingress Controller setup (one-time, per cluster)

```bash
# 1. Install the AWS Load Balancer Controller via Helm
helm repo add eks https://aws.github.io/eks-charts
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=$(terraform output -raw eks_cluster_name) \
  --set serviceAccount.create=true

# 2. Apply the ingress
kubectl apply -f gateway-ingress.yaml

# 3. Point your domain's api.<domain> record at the ALB's address (find it with):
kubectl get ingress labourse-gateway-ingress -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
```

Needs an IAM policy attached to the controller's IRSA role — see AWS's own published policy JSON
at https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/main/docs/install/iam_policy.json,
create it with `aws iam create-policy` and reference it in the Helm install's service account
annotation. Not embedded in this repo's Terraform since it's a large, AWS-maintained document
better fetched fresh than copy-pasted stale.
