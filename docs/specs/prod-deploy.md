# Spec: Production profile, Docker image and PostgreSQL tests

## Goal

Make the API deployable to Render with PostgreSQL, and prove on every PR that
the schema and persistence code work on real PostgreSQL, not only on H2.

Out of scope: creating the Render service itself, backups, monitoring.

## Production profile (`application-prod.yaml`)

Activated with `SPRING_PROFILES_ACTIVE=prod` (set in the Docker image).

| Setting                | Source                          | Notes                                   |
|------------------------|---------------------------------|-----------------------------------------|
| Datasource URL         | `DB_URL`                        | JDBC form: `jdbc:postgresql://host:5432/db` |
| Datasource user        | `DB_USERNAME`                   |                                         |
| Datasource password    | `DB_PASSWORD`                   |                                         |
| HTTP port              | `PORT` (default 8080)           | Render sets `PORT`                      |
| JWT secret             | `JWT_SECRET`                    | Unchanged, already required             |
| CORS origins           | `CORS_ALLOWED_ORIGINS`          | The Vercel frontend URL                 |

- Missing `DB_URL`, `DB_USERNAME` or `DB_PASSWORD` stops startup.
- H2 console disabled.
- `server.forward-headers-strategy: framework`, so `Location` headers use the
  public `https://` URL behind Render's proxy.
- Small connection pool (5), suitable for a small Render database.

## Docker image (`Dockerfile`)

- Two stages: build the jar with JDK 21, run it on a JRE 21 image.
- Tests are not run in the image build; CI runs them.
- Runs as a non-root user, `SPRING_PROFILES_ACTIVE=prod`, port 8080.
- `.dockerignore` keeps build output, IDE files and `.git` out of the context.

## PostgreSQL tests (Testcontainers)

- New test dependencies: `spring-boot-testcontainers`,
  `org.testcontainers:testcontainers-postgresql` and
  `org.testcontainers:testcontainers-junit-jupiter` (versions from Spring Boot).
- The book and user persistence adapter tests also run against
  `postgres:17-alpine`, with Flyway and `ddl-auto: validate`, through
  `@ServiceConnection`. The rest of the suite stays on H2.
- Without Docker, these tests are skipped, so `./gradlew test` still works on
  a machine without Docker.
- CI always has Docker; a CI step fails the build if any PostgreSQL test was
  skipped.

## CI

- A second job builds the Docker image, starts it with the prod profile
  against a PostgreSQL service, and waits for `/actuator/health` to be `UP`.

## Acceptance criteria

- [ ] Book and user persistence tests pass on PostgreSQL in CI and are not skipped.
- [ ] Locally without Docker, `./gradlew test` passes with those tests skipped.
- [ ] The Docker image builds in CI and starts with the prod profile against PostgreSQL.
- [ ] Flyway applies V1 on PostgreSQL and schema validation passes at startup.
- [ ] No secret or database credential is committed.
- [ ] AGENTS.md: new migrations must pass the PostgreSQL tests in CI; prod
      environment variables are documented.
