# WorkNear

Hyperlocal marketplace connecting customers with verified local workers.

```
worknear/
├── backend/    Spring Boot microservices (16 modules) + docker-compose, k8s, terraform
└── frontend/   React Native (Expo) app — customer, worker and admin screens
```

## Quick start

```bash
# 1. backend
cd backend
cp .env.example .env          # fill in real values; .env is git-ignored
docker compose up --build     # gateway on :8080, first start takes a couple of minutes

# 2. frontend (new terminal)
cd frontend
npm install
npx expo start                # set the server address under Login -> Server settings
```

Details, the end-to-end test script and known gaps: `frontend/README.md` and `backend/README.md`.

> Keep `.env` out of git. It holds your database password and JWT secret.
