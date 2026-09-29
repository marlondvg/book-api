# Spec: Current user endpoint

## Goal

Let the frontend find out who is logged in (for example to show the email in
the header, or to check on startup that a stored token still works). The JWT
only carries the user id, so the client cannot get this from the token.

## Endpoint

| Method | Path            | Body | Success             |
|--------|-----------------|------|---------------------|
| `GET`  | `/api/users/me` | -    | 200, `UserResponse` |

`UserResponse` is the same record `register` returns: `id`, `email`,
`createdAt`. Never the password hash.

## Rules

- Needs a valid JWT; the user id comes from the `sub` claim (`@CurrentUserId`).
- Use case `GetCurrentUserUseCase.getCurrentUser(userId)` in `application`,
  backed by a new `UserRepository.findById`.
- A valid token whose user no longer exists gives **401**
  (`UnknownUserException`, detail `User no longer exists`), not 404, so the
  frontend handles it like any other expired login: clear the token and
  go to the login page.

## Acceptance criteria

- [ ] With a valid token, `GET /api/users/me` returns that user's id, email and `createdAt`.
- [ ] The response never contains the password hash.
- [ ] Without a token: 401 problem details (existing security config).
- [ ] Valid token for a user that does not exist: 401 problem details.
- [ ] `findById` is covered by the persistence adapter test (and runs on
      PostgreSQL through the `*PostgresTest` subclass once that exists on `main`).
- [ ] Service unit test, `@WebMvcTest` and an end-to-end test with a real token.
- [ ] `./gradlew test` passes.
