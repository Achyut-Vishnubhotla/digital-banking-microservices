# Digital Banking Platform (Java microservices + Next.js)

A learning project: users register, log in, open accounts and transfer money between accounts.

```
Browser ──> Next.js (3000) ──/api/*──> API Gateway (8080) ──> auth-service    (8081) ──> PostgreSQL (authdb)
                                         (JWT check)      └─> account-service (8082) ──> PostgreSQL (accountdb)
                                                                                  └──> Redis (account cache)
```

| Part | Stack | What it demonstrates |
|---|---|---|
| auth-service | Spring Boot, Spring Security (OAuth2 client), JPA, BCrypt, JJWT | Register / login / refresh, GitHub OAuth2 login, short-lived access token + refresh token |
| account-service | Spring Boot, OAuth2 resource server, JPA/Hibernate, PostgreSQL, Redis | Accounts, transfers, caching, idempotency, optimistic locking |
| api-gateway | Spring Cloud Gateway | Routing + early JWT validation |
| frontend | Next.js 14, TypeScript, Material UI, Jest | Login/register, dashboard, transfer form, paginated history |
| infra | Docker, Docker Compose, GitHub Actions | One-command run, CI (tests + build + image push) |

## Run everything

```bash
cp .env.example .env        # then edit the password and JWT_SECRET (32+ chars)
docker compose up --build
```

Open http://localhost:3000, register, open an account with a demo deposit, then open a second
account (or register a second user) and transfer between them using the 12-digit account number.

## OAuth2 ("Continue with GitHub")

- **auth-service is an OAuth2 client** (authorization-code flow with GitHub, via Spring Security `oauth2Login`).
  After GitHub authenticates the user, auth-service maps them to a local user (`gh_<login>`) and issues the
  same access/refresh JWTs as password login. Tokens reach the browser in the URL *fragment*, which is never sent to servers.
- **account-service is an OAuth2 resource server** (`spring-boot-starter-oauth2-resource-server`): it validates
  the bearer token (HS256 signature, expiry, and `type=access`) and maps the `role` claim to authorities.
- Security notes: local registration of `gh_*` usernames is blocked (prevents pre-hijacking), GitHub users are
  *not* auto-linked to local accounts by e-mail, and the temporary handshake session is invalidated after login.

Setup (optional; password login works without it):

1. GitHub -> Settings -> Developer settings -> OAuth Apps -> **New OAuth App**
2. Homepage URL: `http://localhost:3000`; Authorization callback URL: `http://localhost:3000/login/oauth2/code/github`
3. Copy the Client ID and generate a Client secret into `.env` as `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET`
4. `docker compose up --build`, open http://localhost:3000, click **Continue with GitHub**

## API quick test (curl)

```bash
# register (returns tokens)
curl -s -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"alice","email":"alice@example.com","password":"s3cretPass"}'

TOKEN=...   # accessToken from the response
curl -s -X POST localhost:8080/api/accounts -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"initialDeposit":500}'

# transfer (same Idempotency-Key twice = money moves once)
curl -s -X POST localhost:8080/api/transfers -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-1' \
  -d '{"fromAccountId":1,"toAccountNumber":"<12 digits>","amount":25.00}'
```

## Run services locally without Docker

```bash
docker compose up postgres redis -d
export JWT_SECRET='a-long-random-string-of-at-least-32-characters'
(cd auth-service    && mvn spring-boot:run)
(cd account-service && mvn spring-boot:run)
(cd api-gateway     && mvn spring-boot:run)
(cd frontend        && npm install && npm run dev)
```
Defaults use `bank` / `change-me` for Postgres; override with DB_USER / DB_PASSWORD.

## Tests

```bash
cd auth-service    && mvn verify     # JUnit + Mockito
cd account-service && mvn verify     # JUnit + Mockito + Testcontainers (needs Docker running)
cd frontend        && npm test       # Jest + React Testing Library
```

## Design decisions (good interview material)

- **Transfers live in account-service.** Debit + credit + transfer record must be atomic. In one service and
  one database that is a single `@Transactional`; splitting it across services would need a saga/outbox.
- **Idempotency key.** The client sends `Idempotency-Key`; it is stored with a UNIQUE constraint. A retry
  returns the original result. The frontend keeps the same key after a failure and only rotates it after success.
- **Optimistic locking.** `@Version` on `Account`: if two transfers touch the same account at once, one fails
  with HTTP 409 and the client retries, instead of losing an update.
- **Cache-aside with Redis.** Account lookups are cached for 10 minutes and evicted *after commit* so a
  concurrent reader can't re-cache the old balance. `AccountLookup` is a separate bean because Spring caching works
  through proxies (self-invocation would skip the cache).
- **Ownership checks return 404, not 403**, so account IDs can't be probed.
- **Defence in depth.** The gateway checks the JWT, and account-service validates it again as an OAuth2 resource server.

## Known shortcuts (what you'd change for real production)

- Tokens live in `localStorage` (use httpOnly cookies); refresh tokens can't be revoked/rotated.
- One shared HS256 secret for all services (use asymmetric keys / JWKS or a real IdP such as Keycloak, and a secrets manager).
- `ddl-auto: update` instead of Flyway migrations; the "initial deposit" is demo-only.
- No rate limiting, audit log, or double-entry ledger; Redis outages are not handled gracefully.
- Money is `BigDecimal` in the backend, but the frontend parses it as a JS number (fine for a demo).

## Ideas to extend it

1. Flyway migrations. 2. A Claude-powered "spending insights" endpoint. 3. Deploy to AWS (ECS/EC2).
4. Concurrency test that fires 20 parallel transfers at one account. 5. OpenAPI/Swagger docs.
