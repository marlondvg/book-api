# Spec: Authentication (register, login, JWT)

## Goal

Let people create an account and log in, issue a signed JWT on login, and
check that JWT on every protected request, so the book endpoints work for real
clients. Also add CORS for the frontend and Swagger support for bearer tokens.

Out of scope: refresh tokens, logout or token revocation, password reset,
email verification, roles.

## Endpoints

| Method | Path                 | Body              | Success                       |
|--------|----------------------|-------------------|-------------------------------|
| `POST` | `/api/auth/register` | `RegisterRequest` | 201, `UserResponse`           |
| `POST` | `/api/auth/login`    | `LoginRequest`    | 200, `TokenResponse`          |

- `RegisterRequest`: `email` (required, valid format, max 255),
  `password` (required, 8 to 72 characters).
- `LoginRequest`: `email`, `password` (both required).
- `UserResponse`: `id`, `email`, `createdAt`. Never the password hash.
- `TokenResponse`: `accessToken`, `tokenType` (`"Bearer"`), `expiresIn` (seconds).

Registering does not log the user in; the client calls login next.

## Rules

### Users (domain)

- `User`: `id`, `email`, `passwordHash`, `createdAt`.
- Emails are trimmed and lower-cased, so `Ann@Example.com` and
  `ann@example.com` are the same account.
- `PasswordPolicy`: at least 8 characters and at most 72 bytes in UTF-8
  (BCrypt ignores anything after 72 bytes).
- Registering an email that already exists: `EmailAlreadyUsedException` (409).
- Invalid email: `InvalidUserException` (400).
  Password not meeting the policy: `InvalidPasswordException` (400).

### Login

- Wrong password and unknown email give the same answer:
  `InvalidCredentialsException`, 401, detail `Invalid email or password`.
- For an unknown email the password is still hashed once, so response time
  does not reveal whether the account exists.

### Passwords and tokens (infrastructure)

- Passwords are hashed with BCrypt (`PasswordHasher` port).
- Tokens (`TokenIssuer` port) are HS256 JWTs made with Spring Security's
  Nimbus support: `iss` = `app.jwt.issuer`, `sub` = user id, `iat`, `exp`.
- `app.jwt.secret` comes from the `JWT_SECRET` environment variable, must be
  at least 32 bytes, and the app refuses to start without it.
  `app.jwt.ttl` defaults to 1 hour.
- Tests use a dummy secret from `src/test/resources/config/application.yaml`,
  which is not used outside tests.
- Passwords, hashes and tokens are never logged; the `toString()` of requests
  and commands that carry a password hides it.

### Security configuration

- Stateless sessions, no CSRF (bearer tokens are not sent automatically by
  browsers), no form login or HTTP Basic.
- Public: `/api/auth/**`, `/swagger-ui.html`, `/swagger-ui/**`,
  `/v3/api-docs/**`, `/actuator/health`. Everything else needs a valid JWT.
- A missing, invalid or expired token gets 401 with a problem-details body and
  a `WWW-Authenticate: Bearer` header.

### CORS

- `CorsConfig` allows the origins in `app.cors.allowed-origins`
  (env `CORS_ALLOWED_ORIGINS`, default `http://localhost:5173` for Vite).
- Methods `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`; headers `Authorization`
  and `Content-Type`; the `Location` header is exposed.

### Swagger

- The OpenAPI document declares a bearer JWT scheme so Swagger UI can send
  the token.

## Acceptance criteria

- [ ] Register creates a user with a BCrypt hash and a normalized email.
- [ ] Registering the same email twice (any letter case) gives 409.
- [ ] Invalid email or password gives 400 before anything is hashed or saved.
- [ ] Login with correct credentials returns a token whose `sub` is the user id.
- [ ] Wrong password and unknown email both give the same 401.
- [ ] A token signed with another key, or expired, is rejected with 401.
- [ ] End to end: register, log in, then create and list books with the token.
- [ ] No token on a book endpoint: 401 problem details with `WWW-Authenticate`.
- [ ] Health and Swagger are reachable without a token.
- [ ] CORS preflight from an allowed origin succeeds; other origins are refused.
- [ ] Domain, service, persistence, token and web tests as in AGENTS.md.
- [ ] `./gradlew test` passes.
