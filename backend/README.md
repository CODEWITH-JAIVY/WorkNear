# Labourse Searching — Microservices Platform

Full backend broken out from the monolith into 11 services + platform infra.
Every service is independently deployable, has its own database, and communicates
via REST (through Eureka service discovery + the gateway) or async Kafka events.

## Services

| Service | Port | Database | Purpose |
|---|---|---|---|
| admin-service | 8082 | labourAdmin | Staff admin: separate staff login (own JWT), bootstrap admin, disputes, moderation |
| api-gateway | 8080 | — | Single entry point, JWT validation, rate limiting, routing |
| auth-service | 8081 | labourseAuthService | Signup/login (local + Google OAuth2), JWT issue+refresh, publishes `user.registered` |
| catalog-service | 8093 | labourCatalog | Service taxonomy: 25 categories, 336 bookable services (source of truth for job posting + labour skills) |
| chat-service | 8092 | labourChat | Per-job WebSocket chat room between customer and accepted labour, persisted history |
| config-server | 8888 | — | Present but not used — services read their own application.yml + env vars |
| customer-service | 8083 | labourCustomer | Customer profile, address |
| discovery-server | 8761 | — | Eureka service registry |
| job-service | 8085 | labourJob | Job posting, workers show interest, customer selects one (Redis lock guards double-tap), bulk hiring, job attachments + completion; publishes job events |
| kyc-service | 8091 | labourKyc | Aadhaar/PAN/selfie document review, promotes labour to KYC-verified once all required docs pass |
| labour-service | 8084 | labourLabour | Labour profile, skills, location/availability sync to matching-service |
| matching-service | 8086 | Redis | Redis GEO radius search + attribute filters (service code, KYC, gender, language, budget) |
| media-service | 8087 | — | Profile photos, work photos, KYC docs upload |
| notification-service | 8086 | — | WebSocket push + Kafka consumer, notifies matched labour / customer |
| payment-service | 8089 | labourPayment | Razorpay order creation, webhook, wallet, commission split |
| rating-service | 8090 | labourRating | Bidirectional reviews, pushes cached avg rating to labour-service |

## Event flow (Kafka topics)

```
signup ──▶ user.registered ──▶ customer-service / labour-service (create profile shell)
job post ──▶ matching-service (sync REST call) ──▶ job.posted ──▶ notification-service (push to candidates)
job accept ──▶ Redis Lua atomic lock ──▶ job.accepted ──▶ notification-service (push to customer)
job payment ──▶ payment-service (Razorpay) ──▶ webhook ──▶ wallet credited
job review ──▶ rating-service ──▶ internal call ──▶ labour-service (cached rating updated)
kyc docs approved (all required types) ──▶ kyc-service ──▶ internal call ──▶ labour-service
                                          (kycVerified=true) ──▶ matching-service Redis cache
                                          (unverified labour never gets matched to a job)
job accepted ──▶ both sides can now connect to chat-service's /ws/chat?jobId=X&userId=Y
```

## Run locally

```bash
cp .env.example .env        # fill in real values (DB creds, JWT secret, Razorpay, AWS)
docker compose up --build
```

First boot: each service creates its own MySQL database on startup (`createDatabaseIfNotExist=true` in the compose file), and `spring.jpa.hibernate.ddl-auto: update` creates the tables. Images are built **inside Docker** with `Dockerfile.service` — no local `mvn package` needed. Allow ~90 s on first run.

| Component | URL |
|---|---|
| API Gateway (single entry point) | http://localhost:8080 |
| Eureka dashboard | http://localhost:8761 |
| Config server | http://localhost:8888 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |

## Cross-service rules followed throughout

- **No JPA relations across service boundaries.** Every cross-service reference (`userId`,
  `labourId`, `customerId`) is a plain `Long` FK, resolved via REST/Kafka — never a join.
  This is the #1 thing that breaks when people "migrate" a monolith without re-thinking it.
- **Every service owns its database.** No service reads another service's tables directly.
- **Internal-only endpoints** (`/internal/**`) exist for service-to-service writes (e.g.
  rating-service updating labour-service's cached rating). Never routed through the gateway —
  see "Internal-endpoint security" below for how these are protected.
- **Identity flows once.** The gateway validates the JWT and forwards `X-User-Id` /
  `X-User-Role` headers; downstream services trust the gateway and never re-validate tokens.

## Internal-endpoint security (two phases)

**Phase 1 — shared secret header (done, this pass).** Every service exposing `/internal/**`
runs an `InternalServiceSecretFilter` that rejects any request to that path without a matching
`X-Internal-Secret` header. Every client that calls an internal endpoint (`rating-service` →
labour-service, `kyc-service` → labour-service, `admin-service` → auth-service) now sends it.
One env var, `INTERNAL_SERVICE_SECRET`, shared by all of them — set it in `.env` before running
`docker compose up`. This is a stopgap: the secret is the same for every service, and if one
service's env is compromised, so is every internal endpoint.

**Phase 1.5 — network-level isolation (done, k8s only).** `k8s/network-policies/` restricts,
at the Kubernetes network layer, exactly which pods can even reach an internal port — e.g. only
`admin-service` can reach `auth-service`'s port for anything other than gateway traffic. This
doesn't apply in plain `docker compose` (Docker's bridge network has no equivalent without extra
tooling) — it's a k8s-only layer, apply with `kubectl apply -f k8s/network-policies/`.

**Phase 2 — mTLS (not done, real production answer).** A shared secret is still a static
credential — anyone who reads it from one compromised pod's env can call any other service.
mTLS gives each service its own cert-based identity instead. The practical path once you're on
k8s: install a service mesh (**Linkerd is the simplest to adopt** — `linkerd install | kubectl
apply -f -`, then annotate each deployment's pod template with
`annotations: { linkerd.io/inject: enabled }`); it automatically wraps every pod-to-pod call in
mTLS with no application code changes — the `InternalServiceSecretFilter` can stay as a second
layer or be removed once mesh mTLS is confirmed working. Istio is the heavier, more configurable
alternative if you need more than mTLS (traffic shaping, canary rollouts) later.

## What's still a placeholder / needs real work

- [ ] `notification-service`, `media-service`, `chat-service`: no tests yet — need Testcontainers-based integration tests (WebSocket + Kafka + S3 are awkward to unit test meaningfully)
- [ ] `chat-service`: history endpoint doesn't verify the requester is actually a job participant
- [ ] `chat-service` / `notification-service`: session maps are in-memory — move to Redis before running >1 replica, and add FCM push for offline mobile users
- [ ] No Flyway/Liquibase — `ddl-auto: update` is dev-only, switch before prod
- [ ] KYC verification service not built — flagged as needed in the earlier architecture doc, out of scope for this pass
- [ ] Chat service not built — same
- [ ] Kafka is single-broker in docker-compose — move to managed Kafka with replication ≥3 before real traffic
- [ ] mTLS between services not set up yet (Phase 2 below — shared-secret Phase 1 is done)

## What's done in this pass (previously placeholders)

- ✅ **KYC-verification-service** — labour submits Aadhaar/PAN/selfie docs (uploaded via
  media-service, URL passed in), admin reviews each doc individually. Once every required doc
  type is `VERIFIED`, it automatically flips `labour-service`'s `kycVerified` flag AND pushes it
  into `matching-service`'s Redis cache — **an unverified labour physically cannot be matched to
  a job**, the filter checks it alongside type/availability/rating.
- ✅ **Chat-service** — real per-job WebSocket rooms (`/ws/chat?jobId=X&userId=Y`), persisted
  history so it survives reconnects, deliver-if-online + always-persist semantics. History
  endpoint (`GET /api/chat/jobs/{jobId}/messages`) still needs a participant check before prod
  (flagged inline — right now anyone with a job ID can read its transcript).
- ✅ **OAuth2 (Google) login** — `auth-service` now has the full flow: `OAuth2LoginSuccessHandler`
  issues a token and redirects new users to `/select-role` (frontend collects CUSTOMER/LABOUR),
  which calls `POST /api/auth/select-role` — that's the point `user.registered` actually fires
  for OAuth users. Local signup still fires it immediately since the role is known upfront.
- ✅ **`admin-service`'s ban action** — `auth-service` now has `PUT /internal/auth/{id}/ban`,
  and `login()` rejects disabled/banned users with a clear error.
- ✅ **Razorpay webhook signature verification** — `WebhookVerificationService` does HMAC-SHA256
  verification against the raw request body before trusting any payload; previously this was a
  bare TODO that accepted anything.
- ✅ **Test suites started**:
  - `auth-service`: Mockito unit tests for signup (new/duplicate email), login (bad password,
    banned account)
  - `job-service`: Mockito unit tests for the accept-race outcomes (win/lose/already-closed),
    plus job posting → matching-service dispatch
  - `job-service`: a **Testcontainers integration test** (`JobAcceptanceConcurrencyIT`) that fires
    20 threads at the same job simultaneously against a real Redis container and asserts exactly
    one wins — this is the one test that actually proves the atomicity claim, since Mockito can't
    fake Lua's atomicity guarantee. Run with `mvn -pl job-service test` (needs Docker available).
  - `customer-service`, `labour-service`: profile CRUD + the dual-write to matching-service
    (location/availability must land in both the local DB and the Redis cache, or matching goes stale)
  - `payment-service`: commission-split math on wallet credit (including the "adds to existing
    balance, never overwrites" case — the bug class that silently loses a labour's prior earnings),
    plus a dedicated `WebhookVerificationServiceTest` proving tampered/wrong signatures are rejected
  - `rating-service`: duplicate-review prevention, and that only CUSTOMER→LABOUR reviews propagate
    a cached rating (LABOUR→CUSTOMER reviews correctly don't touch labour-service)
  - `admin-service`: dispute lifecycle (raise → resolve/reject)
  - `kyc-service`: the promotion logic — labour is only marked KYC-verified once ALL required
    doc types are individually VERIFIED, never on a partial set, never on a rejection
  - `matching-service`: the filter decision logic itself — proves an unverified-KYC or unavailable
    labour is excluded even when nearby and type-matched, and that ties break by rating

Coverage is now on every service except `notification-service`, `media-service`, and `chat-service`
(all three are I/O-heavy — WebSocket/S3/Kafka — better suited to integration tests with
Testcontainers than unit tests; not built in this pass).

## Service catalog, multi-skill matching, job-post flow

**Catalog.** `catalog-service` owns the taxonomy (25 categories → 336 leaf services, seeded from
`catalog-service/src/main/resources/db/catalog-seed.sql` on startup, idempotent). The app's first
screen reads `GET /api/catalog/categories`; tapping one reads `/categories/{code}/items`, already
grouped by sub-heading (Electrical / Plumbing / ...). Browsing is open to unauthenticated users.

**Multi-skill matching.** A labour profile now holds *many* catalog codes (`serviceCodes`); a job
asks for exactly *one* (`requiredLabourType` is now a catalog code like `HOME_REPAIR__ELECTRICIAN`).
matching-service matches when that code is in the labour's list. Both sides validate codes against
the catalog before saving, so a typo or made-up code is rejected up front instead of silently
never matching. Profiles synced before this change have no `serviceCodes` and match nothing until
re-saved (fails closed, covered by a test).

**Job post flow.** service → description → photos/video/voice note (max 5) → location → date & time
(`scheduledAt`) → budget → post. Attachments are stored as S3 keys. Retention has two halves:
`POST /api/jobs/{id}/complete` publishes `job.completed` and media-service deletes the files
immediately; an S3 lifecycle rule (`terraform/s3.tf`) expires `job-attachments/` after 2 days as a
backstop. Caveats: S3 lifecycle runs on whole days, so real expiry is ~2-3 days, not exactly 48h;
and a job scheduled more than 2 days out loses its attachments before the work happens.
media-service only accepts uploads to an allowlisted set of folders now, and only ever deletes
keys under `job-attachments/`.

**Not verified.** This environment can't reach Maven Central, so none of this has been compiled or
tested end-to-end. All 79 Java files pass a parse-only syntax check; type-checking and the test
suites need a real `mvn verify` on your machine.

## Changes made while building the React Native client

- **Gateway WebSocket routes.** `/ws/**` used to sit on the notification route as a plain `lb://` HTTP
  route, which cannot proxy a WebSocket upgrade (Spring Cloud Gateway needs `lb:ws://`), and it also
  swallowed `/ws/chat`. There are now two dedicated routes: `lb:ws://NOTIFICATION-SERVICE` for
  `/ws/notifications` and `lb:ws://CHAT-SERVICE` for `/ws/chat`. Not yet exercised against a running gateway.
- **`GET /api/labour/{userId}/public`.** A customer comparing interested workers had no way to see
  anything about them. Returns name, skills, rating, KYC status, city, languages, rate and bio — and
  deliberately no location, gender or contact details.
- Every endpoint the mobile app calls was cross-checked against the controller mappings above and the
  gateway routes (28/28 present).

## Changes made when merging into this tree

Your working tree was the base; only new work was layered on top (your staff-auth `admin-service`,
`S3Config`, auth-service changes and compose file are untouched in substance). Then:

- **`bootstrap.yml` removed everywhere.** Spring Boot 3 does not read it without the bootstrap starter. Every service's
  `application.yml` now carries `spring.application.name` and `spring.cloud.config.enabled: false`.
  `job-service`, `labour-service` and `notification-service` had *no* application name outside `bootstrap.yml`, so Eureka/gateway
  could not have found them.
- **Dockerfiles.** `Dockerfile.service` copied the jar from the *host* `target/` (no `--from=build`), so the Maven stage's output was
  thrown away. Fixed, and every module's `Dockerfile` is now this same correct file (`job-`, `labour-`, `notification-service` still had
  the old prebuilt-jar version).
- **Kafka (three real bugs).** `job-service` and `kyc-service` produced with the default *String* serializer, so sending a Map would throw
  (job posting would return 500; KYC approval would roll back). `labour-service` and `notification-service` consumed with a JSON deserializer
  that requires type headers which `auth-service` deliberately doesn't send, so labour profiles would never be created and no push would arrive.
  They now use the same producer/consumer settings you already use in `auth-service` / `customer-service`; `media-service` too (new listener).
- **`labour-service`, `job-service` and `catalog-service`** gained `spring.jpa.hibernate.ddl-auto: update` (without it Hibernate creates no tables).
- **`catalog-service`** (new): 25 categories / 336 services, own database `labourCatalog`, added to compose.
- **Gateway:** `/ws/notifications` and `/ws/chat` are now `lb:ws://` routes (plain `lb://` cannot proxy a WebSocket handshake), plus the catalog route.
  **Not yet exercised against a running gateway.**
- **New endpoints/features:** browse-and-choose jobs, bulk hiring, job attachments with delete-on-complete, `GET /api/labour/{id}/public`.

Not verified here: nothing in this tree has been compiled or run by me (no Maven Central access). It passes a Java parse check and YAML checks only.

## Deploying to AWS

`terraform/` provisions the actual AWS infrastructure — EKS, RDS, ElastiCache, ECR, S3+CloudFront,
Secrets Manager, Route53/ACM, and a GitHub Actions OIDC deploy role. Kafka runs on the cluster via
Strimzi (`k8s/kafka/`) rather than AWS MSK — see that folder's README for why. Start with
`terraform/README.md` — it has the full bootstrap-to-deployed walkthrough, including the honest
monthly cost estimate (~$150-250/month even at zero traffic — `terraform destroy` between work
sessions if you're not actively testing against it).

## Migrating what's left from the old monolith

Anything not listed above (e.g. full CustomerProfile fields like embedded `LiveLocation`,
`Address` entity richness, OAuth2) — copy the pattern from `customer-service` or
`labour-service`: entity → repository → dto → service → controller, with cross-service
references as plain `Long` ids, never JPA relations.
