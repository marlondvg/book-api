# Spec: Book domain model

## Goal

Model a user's book and its reading lifecycle as pure Java in `domain`, so every
status and rating rule lives in one place and can be tested without Spring.
This feature also adds the ArchUnit tests that enforce the layer rules from
`AGENTS.md`, so later features are checked from the start.

Out of scope: use cases, persistence, REST endpoints, and the `User` model
(it comes with the authentication feature).

## Model

`Book` (aggregate, mirrors table `books` from `V1__create_users_and_books.sql`):

| Field        | Type            | Notes                                   |
|--------------|-----------------|-----------------------------------------|
| `id`         | `UUID`          | Generated on creation                   |
| `ownerId`    | `UUID`          | Required, never changes                 |
| `title`      | `String`        | Required, not blank, max 255            |
| `author`     | `String`        | Required, not blank, max 255            |
| `pages`      | `Integer`       | Optional, > 0                           |
| `isbn`       | `String`        | Optional, max 20                        |
| `coverUrl`   | `String`        | Optional, max 500                       |
| `status`     | `ReadingStatus` | `TO_READ` on creation                   |
| `rating`     | `Rating`        | Optional                                |
| `startedAt`  | `LocalDate`     | Optional                                |
| `finishedAt` | `LocalDate`     | Optional                                |
| `createdAt`  | `Instant`       | Set on creation                         |

`ReadingStatus`: `TO_READ`, `READING`, `READ`, `ABANDONED`.

`Rating`: value object, integer from 1 to 5.

## Business rules

### Status transitions

| From \ To   | TO_READ | READING | READ | ABANDONED |
|-------------|:-------:|:-------:|:----:|:---------:|
| `TO_READ`   |    =    |   yes   | yes  |    yes    |
| `READING`   |   yes   |    =    | yes  |    yes    |
| `READ`      |   no    |   yes   |  =   |    no     |
| `ABANDONED` |   yes   |   yes   |  no  |     =     |

- `=`: changing to the current status does nothing.
- `no`: throws `InvalidStatusTransitionException` (HTTP 409 later).
- `READ -> READING` is a re-read. `ABANDONED -> READING` resumes the book.
  `ABANDONED -> READ` must go through `READING` first.

### Side effects of a status change (given "today")

- To `READING`: sets `startedAt = today` if it is empty; clears `finishedAt`.
- To `READ`: sets `finishedAt = today`.
- To `TO_READ`: clears `startedAt` and `finishedAt`.
- To `TO_READ` or `READING`: clears `rating`.

### Rating

- `Rating` accepts only 1 to 5, otherwise `InvalidRatingException` (HTTP 400 later).
- A book can be rated only when its status is `READ` or `ABANDONED`,
  otherwise `RatingNotAllowedException` (HTTP 409 later).
- The rating can be removed at any time.

### Details

- `title` and `author` are trimmed, required and not blank.
- Optional text fields that are blank are stored as `null`.
- Invalid details throw `InvalidBookException` (HTTP 400 later).
- A `Book` restored from storage is checked against the same rules, so an
  inconsistent row fails fast.

## Acceptance criteria

- [ ] A new book starts as `TO_READ` with no rating or dates.
- [ ] Every cell of the transition table is covered by a unit test.
- [ ] Every side effect above is covered by a unit test.
- [ ] Rating a `TO_READ` or `READING` book is rejected; rating a `READ` or `ABANDONED` book works.
- [ ] `Rating` rejects 0 and 6 and accepts 1 and 5.
- [ ] `domain` depends only on `java.*` (ArchUnit).
- [ ] ArchUnit rules for `application`, controllers and JPA entities are in place.
- [ ] `./gradlew test` passes.
