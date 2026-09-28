# Spec: Book use cases (application layer)

## Goal

Expose everything a user can do with their own books as use cases in
`application`, backed by an outbound repository port. Controllers and
persistence adapters come in later features and plug into these ports.

Out of scope: REST endpoints, JPA adapter, authentication, `User`.

## Use cases (`application/port/in`)

| Interface                  | Method(s)                                      | Returns      |
|----------------------------|------------------------------------------------|--------------|
| `CreateBookUseCase`        | `createBook(CreateBookCommand)`                | `Book`       |
| `GetBookUseCase`           | `getBook(ownerId, bookId)`                     | `Book`       |
| `ListBooksUseCase`         | `listBooks(ownerId, status)`                   | `List<Book>` |
| `UpdateBookDetailsUseCase` | `updateDetails(UpdateBookDetailsCommand)`      | `Book`       |
| `ChangeBookStatusUseCase`  | `changeStatus(ChangeBookStatusCommand)`        | `Book`       |
| `RateBookUseCase`          | `rate(RateBookCommand)`, `clearRating(ownerId, bookId)` | `Book` |
| `DeleteBookUseCase`        | `deleteBook(ownerId, bookId)`                  | nothing      |

Commands are records in `port/in`. Every command and method takes the
`ownerId`, which the web layer will read from the JWT `sub` claim.

`listBooks` takes an optional `status` filter (`null` means all statuses).

## Outbound port (`application/port/out`)

`BookRepository`:

- `Book save(Book book)`
- `Optional<Book> findByIdAndOwnerId(UUID id, UUID ownerId)`
- `List<Book> findAllByOwnerId(UUID ownerId, ReadingStatus status)`
  (`status` may be `null`; ordered by `createdAt` descending)
- `void delete(Book book)`

## Business rules

- Every lookup is scoped by `ownerId`. A book that does not exist and a book
  owned by someone else are treated the same way: `BookNotFoundException`
  (HTTP 404 later, never 403).
- The service holds no business rules of its own. Status, rating and details
  rules stay in `Book`; the service loads, calls the domain, and saves.
- "Now" and "today" come from an injected `java.time.Clock`, so tests are
  deterministic.
- Rating values are turned into `Rating` in the service, so an invalid value
  fails with `InvalidRatingException` before the book is loaded.

## Wiring

`BookService` implements all the use cases and uses `@Transactional`
(read-only for queries). It is **not** a Spring bean yet: there is no
`BookRepository` implementation, so registering it now would break
application startup. The persistence feature adds `@Service` together with
the JPA adapter and a `Clock` bean.

## Acceptance criteria

- [ ] Creating a book saves a `TO_READ` book owned by the caller, with `createdAt` from the clock.
- [ ] Get, update, change status, rate, clear rating and delete throw `BookNotFoundException`
      when the repository finds no book for that owner.
- [ ] Changing status uses today's date from the clock.
- [ ] Domain errors are not caught or wrapped by the service.
- [ ] Nothing is saved or deleted when a rule fails.
- [ ] Unit tests with Mockito cover every use case.
- [ ] ArchUnit: `application` depends only on `domain`, `java.*`, and Spring's
      `stereotype` and `transaction.annotation` packages.
- [ ] `./gradlew test` passes.
