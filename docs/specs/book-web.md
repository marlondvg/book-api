# Spec: Book REST API (web layer)

## Goal

Expose the book use cases over HTTP under `/api/books`, with request
validation, one consistent error body, and the current user taken from the
JWT `sub` claim.

Out of scope: login, registration, issuing and verifying tokens, CORS
(the authentication feature adds them).

## Endpoints

All endpoints need an authenticated user. Every one of them acts only on the
caller's books.

| Method   | Path                        | Body                 | Success               |
|----------|-----------------------------|----------------------|-----------------------|
| `POST`   | `/api/books`                | `BookDetailsRequest` | 201 + `Location`, book |
| `GET`    | `/api/books?status=READING` | -                    | 200, list of books    |
| `GET`    | `/api/books/{id}`           | -                    | 200, book             |
| `PUT`    | `/api/books/{id}`           | `BookDetailsRequest` | 200, book             |
| `PUT`    | `/api/books/{id}/status`    | `StatusRequest`      | 200, book             |
| `PUT`    | `/api/books/{id}/rating`    | `RatingRequest`      | 200, book             |
| `DELETE` | `/api/books/{id}/rating`    | -                    | 200, book             |
| `DELETE` | `/api/books/{id}`           | -                    | 204                   |

`status` on the list endpoint is optional; without it all books are returned,
newest first.

### Requests

- `BookDetailsRequest`: `title` (required, max 255), `author` (required,
  max 255), `pages` (optional, > 0), `isbn` (optional, max 20),
  `coverUrl` (optional, max 500).
- `StatusRequest`: `status` (required, one of the `ReadingStatus` names),
  `timeZone` (optional IANA zone ID used for `startedAt` / `finishedAt`; see
  `status-date-timezone.md`).
- `RatingRequest`: `value` (required, 1 to 5).

### Response (`BookResponse`)

`id`, `title`, `author`, `pages`, `isbn`, `coverUrl`, `status`, `rating`
(number or `null`), `startedAt`, `finishedAt` (ISO dates), `createdAt`
(ISO instant). `ownerId` is never returned.

## Current user

- A `@CurrentUserId UUID` controller parameter is resolved from the JWT `sub`
  claim of the authenticated request (`infrastructure/security`).
- No JWT authentication, or a `sub` that is not a UUID, gives 401.
- `ownerId` is never read from the path, query or body.

## Errors

One `@RestControllerAdvice` returns RFC 9457 problem details
(`application/problem+json`) with `type`, `title`, `status`, `detail` and
`instance`:

| Cause                                                   | Status |
|---------------------------------------------------------|--------|
| Bean Validation failure (adds an `errors` list of `field`, `message`) | 400 |
| Malformed JSON, unknown status name, invalid UUID       | 400    |
| `InvalidBookException`, `InvalidRatingException`        | 400    |
| No current user                                         | 401    |
| `BookNotFoundException` (missing **or** another user's) | 404    |
| `InvalidStatusTransitionException`, `RatingNotAllowedException` | 409 |
| Anything else                                           | 500, generic message, logged |

Stack traces and exception class names are never sent to clients.

## Acceptance criteria

- [ ] Each endpoint returns the success status and body above (`@WebMvcTest`).
- [ ] The use cases receive the owner id from the JWT, never from the request.
- [ ] Validation errors return 400 with the failing fields.
- [ ] Domain errors map to 400, 404 and 409 as listed.
- [ ] Requests without authentication get 401.
- [ ] End to end (`@SpringBootTest`): user A cannot read, update, change the
      status of, rate or delete user B's book; each attempt gets 404 and
      user B's book is unchanged.
- [ ] Controllers only use `port/in` use cases and return DTOs (ArchUnit).
- [ ] `./gradlew test` passes.
