# book-api

REST API for a personal book reading tracker. People create an account, log in,
and keep track of **their own** books: what they want to read, are reading,
have finished or gave up on, with dates and a rating.

The frontend lives in a separate repository (`book-app-frontend`, React + Vite).

## Stack

- Java 21, Spring Boot 4.1, Gradle
- Spring Web, Spring Data JPA, Bean Validation
- Spring Security with JWT (HS256), BCrypt passwords
- Flyway migrations; H2 (PostgreSQL mode) locally, PostgreSQL in production
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, ArchUnit, Testcontainers

## Run it locally

Requirements: JDK 21 installed. No database needed: the app uses an in-memory
H2 database.

```bash
JWT_SECRET=$(openssl rand -base64 48) ./gradlew bootRun
```

The app starts on http://localhost:8080. It refuses to start without
`JWT_SECRET`. A new random value per run is fine locally, but tokens from a
previous run stop working, and the in-memory data is gone after a restart.

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

Try it with `curl` (the login line uses [`jq`](https://jqlang.org) to read the token):

```bash
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email": "ann@example.com", "password": "correct horse battery"}'

TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email": "ann@example.com", "password": "correct horse battery"}' | jq -r .accessToken)

curl -X POST localhost:8080/api/books -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"title": "Dune", "author": "Frank Herbert"}'

curl localhost:8080/api/books -H "Authorization: Bearer $TOKEN"
```

In Swagger UI, log in through `/api/auth/login`, then click **Authorize** and
paste the `accessToken`.

## API

All endpoints are under `/api` and use JSON. Everything except `/api/auth/**`
needs an `Authorization: Bearer <token>` header. The user always comes from the
token; a user can only see and change their own books.

### Auth

| Method | Path                 | Body                  | Response                                        |
|--------|----------------------|-----------------------|-------------------------------------------------|
| `POST` | `/api/auth/register` | `{email, password}`   | 201 `{id, email, createdAt}`                    |
| `POST` | `/api/auth/login`    | `{email, password}`   | 200 `{accessToken, tokenType, expiresIn}`       |
| `GET`  | `/api/users/me`      | -                     | 200 `{id, email, createdAt}`                    |

- Passwords: 8 to 72 characters. Emails are case-insensitive.
- Tokens last 1 hour. There are no refresh tokens; log in again.
- After 5 failed logins for an email, or 20 from one IP, within 15 minutes,
  login answers **429** with a `Retry-After` header until the window ends.

### Books

| Method   | Path                          | Body                                     | Response          |
|----------|-------------------------------|------------------------------------------|-------------------|
| `POST`   | `/api/books`                  | `{title, author, pages?, isbn?, coverUrl?}` | 201 book + `Location` |
| `GET`    | `/api/books?status=READING`   | -                                        | 200 list, newest first; `status` optional |
| `GET`    | `/api/books/{id}`             | -                                        | 200 book          |
| `PUT`    | `/api/books/{id}`             | `{title, author, pages?, isbn?, coverUrl?}` | 200 book       |
| `PUT`    | `/api/books/{id}/status`      | `{status}`                               | 200 book          |
| `PUT`    | `/api/books/{id}/rating`      | `{value}` (1 to 5)                       | 200 book          |
| `DELETE` | `/api/books/{id}/rating`      | -                                        | 200 book          |
| `DELETE` | `/api/books/{id}`             | -                                        | 204               |

A book looks like this:

```json
{
  "id": "5f0c6a52-2b8e-4b7e-9d3f-1c2a3b4c5d6e",
  "title": "Dune",
  "author": "Frank Herbert",
  "pages": 412,
  "isbn": "9780441013593",
  "coverUrl": null,
  "status": "READ",
  "rating": 5,
  "startedAt": "2026-02-01",
  "finishedAt": "2026-03-01",
  "createdAt": "2026-01-15T10:00:00Z"
}
```

### Reading status rules

Statuses: `TO_READ` (new books start here), `READING`, `READ`, `ABANDONED`.

| From \ To   | TO_READ | READING | READ | ABANDONED |
|-------------|:-------:|:-------:|:----:|:---------:|
| `TO_READ`   |    -    |   yes   | yes  |    yes    |
| `READING`   |   yes   |    -    | yes  |    yes    |
| `READ`      |   no    |   yes   |  -   |    no     |
| `ABANDONED` |   yes   |   yes   |  no  |     -     |

- Moving to `READING` sets `startedAt` if it is empty. Moving to `READ` sets `finishedAt`.
- Moving back to `TO_READ` clears both dates.
- A rating (1 to 5) is only allowed for `READ` or `ABANDONED` books. Moving to
  `TO_READ` or `READING` removes it.

### Errors

Every error uses the same [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)
problem-details body (`application/problem+json`):

```json
{
  "title": "Conflict",
  "status": 409,
  "detail": "Cannot change status from READ to TO_READ",
  "instance": "/api/books/5f0c6a52-2b8e-4b7e-9d3f-1c2a3b4c5d6e/status"
}
```

| Status | When                                                                  |
|--------|-----------------------------------------------------------------------|
| 400    | Invalid input. Validation errors add an `errors` list of `{field, message}` |
| 401    | Missing, invalid or expired token; wrong email or password; the token's user no longer exists |
| 404    | The book does not exist **or** belongs to another user                |
| 409    | Status change not allowed, rating not allowed yet, email already used |
| 429    | Too many failed logins; see `Retry-After`                             |

## Configuration

| Variable               | Required            | Description                                             |
|------------------------|---------------------|---------------------------------------------------------|
| `JWT_SECRET`           | Always              | Key that signs tokens. At least 32 bytes, random        |
| `CORS_ALLOWED_ORIGINS` | No                  | Comma-separated frontend URLs (default `http://localhost:5173`) |
| `SPRING_PROFILES_ACTIVE` | Production        | `prod` (already set in the Docker image)                |
| `DB_URL`               | Production          | JDBC URL, e.g. `jdbc:postgresql://host:5432/books`      |
| `DB_USERNAME`          | Production          | Database user                                           |
| `DB_PASSWORD`          | Production          | Database password                                       |
| `PORT`                 | No                  | HTTP port (default 8080; set by Render)                 |

Login limits and token lifetime can be tuned under `app.login-limit` and
`app.jwt` in `src/main/resources/application.yaml`.

## Tests

```bash
./gradlew test    # all tests
./gradlew build   # compile, test and package
```

- Unit tests for the domain and services, `@WebMvcTest` for controllers,
  `@DataJpaTest` for persistence, end-to-end tests with real tokens, and
  ArchUnit tests for the layer rules.
- The persistence tests also run on real PostgreSQL through Testcontainers
  (`*PostgresTest`). Without Docker they are skipped locally; CI always runs them.

CI (GitHub Actions) runs on every pull request: the full build, plus a job that
builds the Docker image and starts it against PostgreSQL.

## Deployment

The backend runs on [Render](https://render.com) from the `Dockerfile`, with a
Render PostgreSQL database. The frontend runs on Vercel.

1. Create a PostgreSQL database on Render.
2. Create a Web Service from this repository; Render builds the `Dockerfile`.
3. Set the environment variables from [Configuration](#configuration). Render
   shows the database URL as `postgresql://user:pass@host/db`; write it as
   `jdbc:postgresql://host:5432/db` in `DB_URL` and put the user and password
   in `DB_USERNAME` and `DB_PASSWORD`. Use a new random `JWT_SECRET`
   (`openssl rand -base64 48`).
4. Set the health check path to `/actuator/health`.

Flyway applies the database migrations on startup.

## Project structure

The code follows a pragmatic hexagonal (ports and adapters) layout:

```
src/main/java/dev/marlondvg/book_api
├── domain/          Book, User, reading rules; plain Java
├── application/     Use cases (port/in), outbound ports (port/out), services
└── infrastructure/  Web controllers, JPA persistence, security, configuration
```

Specs for each feature are in [`docs/specs`](docs/specs). Conventions for
contributors (and AI coding agents) are in [`AGENTS.md`](AGENTS.md).
