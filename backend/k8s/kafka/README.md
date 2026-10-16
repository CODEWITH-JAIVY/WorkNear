# Kafka on EKS via Strimzi (not AWS MSK)

Chose self-hosted Kafka (via the Strimzi operator) over AWS MSK deliberately: MSK's simplest
auth mode is IAM, which needs the `aws-msk-iam-auth` SASL client added to every service's
`pom.xml` and a SASL block in every `application.yml` — 9 services' worth of extra config for
what a startup at this stage doesn't need. Strimzi keeps `spring.kafka.bootstrap-servers` working
exactly like it does in `docker-compose` today — just point it at the in-cluster service instead
of `kafka:9092`. Revisit MSK once you have an ops person dedicated to not babysitting Kafka.

## Install

```bash
kubectl create namespace kafka
kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
kubectl apply -f kafka-cluster.yaml -n kafka
```

Once ready, every service's `KAFKA_BROKER` env var becomes:
```
labourse-kafka-kafka-bootstrap.kafka.svc.cluster.local:9092
```
