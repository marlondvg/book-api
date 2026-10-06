# book-api

REST API for a personal book reading tracker. People create an account, log in,
and keep track of **their own** books: what they want to read, are reading,
have finished or gave up on, with dates and a rating.

The frontend lives in a separate repository (`book-app-frontend`, React + Vite).

**Live API:** https://book-api-oasv.onrender.com
([Swagger UI](https://book-api-oasv.onrender.com/swagger-ui.html),
[health](https://book-api-oasv.onrender.com/actuator/health)).
It runs on Render's free plan, which sleeps after a period without traffic, so
the first request after a pause can take about a minute.

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

From IntelliJ, open **Run → Edit Configurations…**, select `BookApiApplication`
and add `JWT_SECRET=<a value of 32+ characters>` under **Environment variables**.
Keep that value out of any run configuration you commit.

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

1. Create a PostgreSQL database on Render (it may be shared with other apps).
   The app keeps all its tables in the `book_tracker` schema and does not
   create it, so create it once before the first deploy:
   `CREATE SCHEMA book_tracker;`. The database user needs `USAGE` and `CREATE`
   on it. The schema must be **empty or used only by this app**: Flyway refuses
   to start on a schema that already holds other tables.
2. Create a Web Service from this repository; Render builds the `Dockerfile`.
3. On the **Web Service** (not the database), set the environment variables
   from [Configuration](#configuration):
   - Copy the database's **Internal Database URL**
     (`postgresql://USER:PASSWORD@HOST/DB`) and split it:
     `DB_URL=jdbc:postgresql://HOST:5432/DB`, `DB_USERNAME=USER`,
     `DB_PASSWORD=PASSWORD`. Use the internal URL, not the external one: both
     services are on Render's private network.
   - `JWT_SECRET`: a new random value (`openssl rand -base64 48`), never one
     used elsewhere.
   - `CORS_ALLOWED_ORIGINS`: the Vercel frontend URL, without a trailing slash.
   - Paste values without quotes or trailing spaces.
4. Set the health check path to `/actuator/health`.

Flyway applies the database migrations on startup. A successful first deploy
logs `Migrating schema "book_tracker" to version "1 - create users and books"`, and
`/actuator/health` then reports `UP`.

Never paste a database URL that contains the password into chats, issues or
logs. If that happens, rotate the database credentials in Render and update
`DB_PASSWORD`.

### Troubleshooting

| Error in the Render log | Cause | Fix |
|---|---|---|
| `'url' must start with "jdbc"` | `DB_URL` was pasted in Render's `postgresql://…` form | Use `jdbc:postgresql://HOST:5432/DB`, with user and password in their own variables |
| `Found non-empty schema(s) "book_tracker" but no schema history table` | The schema holds another app's tables | Use an empty schema dedicated to this app (see step 1) |
| `Schema "book_tracker" does not exist` or `permission denied for schema book_tracker` | The schema was not created, or the database user lacks rights on it | Create it and grant `USAGE` and `CREATE` (see step 1) |
| `app.jwt.secret (JWT_SECRET) must be set and at least 32 bytes long` | `JWT_SECRET` missing, too short, or set on the database instead of the Web Service | Set it on the Web Service |
| `password authentication failed` | `DB_USERNAME` or `DB_PASSWORD` does not match the database | Copy both again from the same database's internal URL |

`No open ports detected, continuing to scan...` while the app starts is normal:
startup takes a while on a small instance, and Render keeps checking until the
port opens.

To look inside the production database from your machine, install the client
(`brew install libpq && brew link --force libpq`) and run
`psql "<External Database URL>"`, copied from the database's **Connect →
External** tab.

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
