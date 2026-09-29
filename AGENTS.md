# AGENTS.md

Guidance for AI coding agents (and humans) working on `book-api`.
Read this file fully before making changes.

## Project

REST API for a personal book reading tracker. Users create an account, log in, and manage **only their own** books (status, rating, dates).
Frontend lives in a separate repo (`book-app-frontend`, React + Vite).

## Stack

- Java 21 (fixed via Gradle toolchain, do not lower it)
- Spring Boot 4.1.x, Gradle (Groovy DSL)
- Spring Web, Spring Data JPA, Validation
- Spring Security + OAuth2 Resource Server (JWT, HS256/Nimbus, no external JWT libs)
- Flyway for schema migrations, `ddl-auto: validate`
- H2 (PostgreSQL mode) for local dev and tests, PostgreSQL in production
- springdoc-openapi 3.0.0 (Swagger UI)
- MapStruct (optional, only in `infrastructure`)
- Tests: JUnit 5, Mockito, Spring Boot Test, ArchUnit

## Commands

```bash
JWT_SECRET=$(openssl rand -base64 48) ./gradlew bootRun   # run the app (http://localhost:8080)
./gradlew test           # run all tests, including architecture tests
./gradlew build          # compile + test + package
```

The app refuses to start without `JWT_SECRET` (at least 32 bytes). A random value
per run is fine locally; tokens issued before a restart then stop working.
Tests use a dummy key from `src/test/resources/config/application.yaml`.
Optional: `CORS_ALLOWED_ORIGINS` (comma-separated, default `http://localhost:5173`).

Swagger UI: `http://localhost:8080/swagger-ui.html`
Health check: `/actuator/health`

## Architecture: pragmatic hexagonal (ports & adapters)

```
dev.marlondvg.book_api
├── domain/            Pure Java: Book, User, ReadingStatus, Rating, domain exceptions
├── application/
│   ├── port/in/       Use case interfaces
│   ├── port/out/      Repository and other outbound ports
│   └── service/       Use case implementations
└── infrastructure/
    ├── web/           Controllers, request/response DTOs (records), exception handler
    ├── persistence/   JPA entities, Spring Data repos, adapters, mappers
    ├── security/      JWT config, password encoder, current-user resolution
    └── config/        CorsConfig, OpenAPI config, other beans
```

### Dependency rules (enforced by ArchUnit, do not break them)

1. `domain` depends on nothing: no Spring, no JPA, no Jackson, no MapStruct, no Lombok.
2. `application` depends only on `domain`. No Spring web or persistence types.
   `@Service` and `@Transactional` are allowed on application services.
3. `infrastructure` may depend on `application` and `domain`. Never the reverse.
4. Controllers never touch JPA entities or Spring Data repositories. They call `port/in` use cases.
5. JPA entities (`*JpaEntity`) never leave `infrastructure/persistence`. Map to domain objects at the adapter boundary.
6. Domain objects are never returned directly from controllers. Use DTOs.

## Domain rules (source of truth)

- `ReadingStatus`: `TO_READ`, `READING`, `READ`, `ABANDONED`.
- `rating` is 1 to 5 and can only be set when status is `READ` or `ABANDONED`.
- Moving to `READING` sets `startedAt` (if empty). Moving to `READ` sets `finishedAt`.
- These rules live in the domain (`Book`), not in controllers or the database alone.
  The DB has matching CHECK constraints as a safety net.
- Invalid transitions throw a domain exception, mapped to HTTP 400/409 in the exception handler.

## Security conventions

- Every endpoint except `/api/auth/**`, Swagger and `/actuator/health` requires a valid JWT.
- The current user id comes from the JWT `sub` claim, never from the request body or path.
- Every book query is scoped by `ownerId`.
- If a book does not exist **or** belongs to another user, respond **404**, never 403.
- Passwords are hashed with BCrypt. Never log passwords, tokens, or password hashes.
- Secrets (JWT key, DB credentials) come from environment variables, never from committed files.

## API conventions

- Base path `/api`. JSON only. Plural resource names (`/api/books`).
- Request and response DTOs are Java `record`s with Bean Validation annotations.
- Errors use a single consistent body, produced by one `@RestControllerAdvice`.
- CORS is configured in `CorsConfig` with allowed origins from configuration.

## Database

- Schema changes only through new Flyway migrations in `src/main/resources/db/migration`
  (`V<n>__description.sql`). Never edit a migration that has already been committed to `main`.
- Must run on both H2 (PostgreSQL mode) and PostgreSQL. Avoid vendor-specific SQL.
- A new migration is not done until the PostgreSQL tests (`*PostgresTest`) pass in CI.

## Testing

- **Domain**: plain unit tests, no Spring context. Cover every status transition and the rating rule.
- **Application services**: unit tests with mocked ports (Mockito).
- **Web**: `@WebMvcTest` slices; use `@MockitoBean` (not `@MockBean`) for use cases.
- **Persistence**: `@DataJpaTest` against H2 with Flyway enabled. Each adapter test also has a
  `*PostgresTest` subclass that reruns it on real PostgreSQL via Testcontainers
  (`PostgresTestcontainersConfig`). They are skipped without Docker locally; CI fails if they are skipped.
- **Architecture**: ArchUnit tests in `src/test/java/.../architecture` for the dependency rules above.
- **Security**: tests must cover "user A cannot read, update, or delete user B's book" (expects 404).
- Test names describe behavior, for example `shouldRejectRatingWhenBookIsToRead`.
- A feature is not done until its tests pass with `./gradlew test`.

## Code style

- Code, comments, commit messages and PR titles in **English**.
- Constructor injection only. No field injection.
- Prefer immutability: records for DTOs and value objects, `final` fields.
- No Lombok. Keep classes small and focused.
- Do not add dependencies without asking; explain why they are needed.

## Workflow

1. Before each feature, write a short spec in `docs/specs/<feature>.md`:
   goal, endpoints or use cases, business rules, acceptance criteria.
2. Implement domain first, then application, then infrastructure.
3. Write or update tests in the same change.
4. Run `./gradlew test` before proposing a commit.
5. Keep changes small: one feature or fix per branch and PR.

### Git

- Conventional Commits: `feat:`, `fix:`, `test:`, `refactor:`, `docs:`, `chore:`.
- Branches: `feat/<name>`, `fix/<name>`. Never commit directly to `main`.
- CI (GitHub Actions) runs `./gradlew build` on every PR.

## Deployment

- Backend on Render using the project `Dockerfile`; frontend on Vercel.
  Live API: `https://book-api-oasv.onrender.com`.
- The production database must be dedicated to this app. Flyway fails on a schema that
  already holds other tables and no history table; never "fix" that with `baselineOnMigrate`
  in committed config.
- Production profile: `spring.profiles.active=prod` (set in the `Dockerfile`), PostgreSQL via environment variables,
  `flyway-database-postgresql` module on the runtime classpath.
- Environment variables in production:
  - `DB_URL` (JDBC form, `jdbc:postgresql://host:5432/db`), `DB_USERNAME`, `DB_PASSWORD`
  - `JWT_SECRET` (at least 32 bytes, random, never reused from another environment)
  - `CORS_ALLOWED_ORIGINS` (the Vercel frontend URL)
  - `PORT` (set by Render)
- CI builds the Docker image and starts it with the prod profile against PostgreSQL on every PR.

## Do not

- Do not put business rules in controllers or JPA entities.
- Do not expose or accept `ownerId` from clients.
- Do not disable Spring Security or CSRF settings "temporarily" and commit it.
- Do not use `ddl-auto: update` or `create`.
- Do not swallow exceptions or return raw stack traces to clients.
