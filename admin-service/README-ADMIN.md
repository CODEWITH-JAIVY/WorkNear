# admin-service (RBAC edition)

Staff (employees) ke liye alag login, roles aur permissions. End users (CUSTOMER/LABOUR) ke tokens se admin
routes nahi chalte, aur staff tokens auth-service ke tokens se alag secret se sign hote hain.

## 1. Environment variables (zaroori)

| Variable | Matlab |
|---|---|
| `DB_PASSWORD` | MySQL root password (pehle jaisa) |
| `ADMIN_JWT_SECRET` | Staff token signing key, **minimum 32 characters**, random rakho. Missing ho to service start nahi hogi |
| `ADMIN_BOOTSTRAP_EMAIL` / `ADMIN_BOOTSTRAP_PASSWORD` | Pehla SUPER_ADMIN, sirf tab banta hai jab `staff_users` table khali ho. Password policy follow karna |
| `SPRING_DATASOURCE_URL`, `EUREKA_URL`, `INTERNAL_SERVICE_SECRET` | Docker mein host names ke liye (compose snippet neeche) |

`docker-compose.yml` mein `admin-service` ke `environment:` mein:
```yaml
      DB_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/labourAdmin?createDatabaseIfNotExist=true
      EUREKA_URL: http://discovery-server:8761/eureka
      ADMIN_JWT_SECRET: ${ADMIN_JWT_SECRET}
      ADMIN_BOOTSTRAP_EMAIL: ${ADMIN_BOOTSTRAP_EMAIL}
      ADMIN_BOOTSTRAP_PASSWORD: ${ADMIN_BOOTSTRAP_PASSWORD}
```
`.env` mein (git mein commit mat karna):
```
ADMIN_JWT_SECRET=<kam se kam 32 random characters>
ADMIN_BOOTSTRAP_EMAIL=owner@labourse.com
ADMIN_BOOTSTRAP_PASSWORD=Change@Me123
```

## 2. Roles aur permissions

| Role | Permissions |
|---|---|
| SUPER_ADMIN | sab kuch, staff create/disable/role change |
| ADMIN | dashboard, disputes (view/assign/resolve), user ban/unban, staff view, audit view |
| CUSTOMER_EXECUTIVE | dashboard, disputes view/resolve, **sirf CUSTOMER wale disputes** |
| LABOUR_EXECUTIVE | dashboard, disputes view/resolve, **sirf LABOUR wale disputes** |
| FINANCE | dashboard, disputes view, refund request + approve (maker-checker) |
| ANALYST | dashboard, disputes view (read-only) |

Naya role: `StaffRole.java` mein ek line. Naya permission: `Permission.java` + controller par `@PreAuthorize`.

## 3. APIs

**Auth** (`/api/admin/auth`): `POST /login`, `POST /refresh`, `POST /logout`, `POST /change-password`, `GET /me`

**Staff** (`/api/admin/staff`, STAFF_MANAGE/VIEW): `POST` create (temporary password sirf ek baar response mein aata hai),
`GET` list, `GET /{id}`, `GET /roles`, `PUT /{id}/role`, `PUT /{id}/disable`, `PUT /{id}/enable`, `POST /{id}/reset-password`

**Disputes (staff)** (`/api/admin/disputes`): `GET ?status=OPEN&page=0&size=20`, `GET /{id}`, `POST /{id}/claim`,
`POST /{id}/assign {"staffId":2}`, `POST /{id}/comments {"text":"..."}`, `POST /{id}/resolve {"notes":"...","accepted":true}`

**Disputes (end user)** (`/api/disputes`): `POST {"jobId":1,"reason":"..."}`, `GET /mine`  (gateway se `X-User-Id`, `X-User-Role` aate hain)

**Refunds** (`/api/admin/refunds`): `POST {"jobId":1,"disputeId":1,"amount":500,"reason":"..."}`, `GET ?status=PENDING`,
`POST /{id}/approve {"notes":"..."}`, `POST /{id}/reject {"notes":"..."}`. Request karne wala khud approve nahi kar sakta.

**Users**: `POST /api/admin/users/{userId}/ban {"reason":"..."}`, `POST /api/admin/users/{userId}/unban {"reason":"..."}`

**Audit**: `GET /api/admin/audit?actorId=1&action=USER_BANNED`  **Dashboard**: `GET /api/admin/dashboard`

Pehle wale `POST /api/admin/disputes?jobId=..&reason=..` (end user ke liye) ab `POST /api/disputes` (JSON body) hai, aur
resolve/ban ab query params nahi, JSON body lete hain.

## 4. Security behaviour

- 5 galat passwords par account 15 minute ke liye lock (`423`).
- Temporary password pehle login par badalna padta hai; tab tak sirf `/auth/me` aur `/auth/change-password` chalte hain.
- Staff disable / password reset / password change par purane access tokens turant invalid ho jaate hain, refresh tokens revoke hote hain.
- Refresh token rotate hota hai; purana token dobara use hua to us staff ke saare refresh tokens revoke.
- Aakhri active SUPER_ADMIN ko disable ya demote nahi kar sakte. Apna role/account khud nahi badal sakte.
- Har sensitive action `audit_logs` table mein (kaun, kab, kis IP se, kya).

## 5. Gateway mein kya badalna hai

1. `/api/admin/**` aur `/api/disputes/**` dono routes `admin-service` par jaayein.
2. Gateway ka JWT filter `/api/admin/**` ko **skip** kare (staff tokens admin-service khud verify karta hai),
   lekin `/api/disputes/**` ko protect kare aur `X-User-Id` + `X-User-Role` forward kare.
3. admin-service ka port sirf docker network ke andar rahe, bahar publish na ho, kyunki `/api/disputes` gateway ke headers par bharosa karta hai.

## 6. Abhi baaki / assumptions

- `unban` ke liye auth-service mein `PUT /internal/auth/{id}/unban` hona chahiye (ban wale jaisa).
- Refund approve sirf decision record karta hai; paise payment-service se wapas karna alag integration hai.
- Customer/labour profile dekhna, KYC approve, rating hatana jaise features tab add honge jab un services ke internal endpoints hon.
- Agar gateway `X-User-Role` nahi bhejta, to disputes ka `raisedByType` `UNKNOWN` hoga aur executives wo disputes nahi dekh paayenge (ADMIN dekh lega).
- Ye code Maven ke saath compile nahi kiya gaya (sandbox mein Maven Central blocked tha), pehla build chalane par agar compile error aaye to bhej do.
