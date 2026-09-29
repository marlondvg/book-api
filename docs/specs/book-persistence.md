# Spec: Book persistence

## Goal

Implement the `BookRepository` outbound port with JPA on the existing `books`
table, and register `BookService` as a Spring bean so the use cases can be
called by the web layer.

Out of scope: REST endpoints, `User` persistence, schema changes.

## Components (`infrastructure/persistence`)

| Class                       | Role                                                     |
|-----------------------------|----------------------------------------------------------|
| `BookJpaEntity`             | JPA mapping of table `books`. Data only, no rules        |
| `SpringDataBookRepository`  | Spring Data interface for `BookJpaEntity`                |
| `BookMapper`                | Converts `Book` <-> `BookJpaEntity`                      |
| `BookPersistenceAdapter`    | Implements `BookRepository` using the two classes above  |

Wiring (`infrastructure/config`):

- `ClockConfig` provides a `Clock` bean (`Clock.systemUTC()`), so "today" is
  the UTC date.
- `BookService` gets `@Service`.

## Rules

- No schema change: the entity must pass `ddl-auto: validate` against
  `V1__create_users_and_books.sql` on H2 (PostgreSQL mode) and PostgreSQL.
- `BookJpaEntity` never leaves `infrastructure/persistence`; the adapter
  returns domain `Book` objects built with `Book.restore(...)`, so data read
  from the database is checked against the domain rules.
- Every query is scoped by `owner_id`:
  - `findByIdAndOwnerId` returns empty for a book owned by someone else.
  - `findAllByOwnerId` returns only the owner's books, newest `created_at`
    first, filtered by status when one is given.
- `status` is stored as the enum name, `rating` as a small integer.

## Acceptance criteria

- [ ] The application context starts with `ddl-auto: validate` and Flyway.
- [ ] A saved book is read back with every field unchanged.
- [ ] Changes to an existing book (details, status, dates, rating) are saved.
- [ ] `findByIdAndOwnerId` returns empty for another owner's book.
- [ ] `findAllByOwnerId` returns only the owner's books, newest first, and
      filters by status.
- [ ] `delete` removes the book.
- [ ] `@DataJpaTest` tests run against H2 in PostgreSQL mode with Flyway.
- [ ] `./gradlew test` passes.
