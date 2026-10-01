# Spec: Status dates in the user's time zone

## Goal

`startedAt` and `finishedAt` must be the user's calendar date, not the date in
UTC. Today the server stamps `LocalDate.now(clock)` with a UTC clock, so a
user in `America/Bogota` who starts a book at 20:00 on Sept 30 gets Oct 1.

Out of scope: storing a time zone per user, letting users pick or backdate
`startedAt` / `finishedAt`, changing `createdAt` (an `Instant`, already correct).

## API change

`PUT /api/books/{id}/status` accepts an optional `timeZone`:

```json
{ "status": "READING", "timeZone": "America/Bogota" }
```

- `timeZone` is an IANA zone ID (what the browser returns from
  `Intl.DateTimeFormat().resolvedOptions().timeZone`). Fixed offsets such as
  `+02:00` and `UTC` are also accepted, since `java.time.ZoneId` parses them.
- Omitted, `null` or `""`: the server's clock zone (UTC) is used, as before. Existing
  clients keep working unchanged.
- Unknown or malformed zone (`"Mars/Olympus"`, `"+25:00"`, `42`): **400** problem
  detail, the same as an unknown `status` name. Nothing is changed.

## Design

- `StatusRequest` gains `ZoneId timeZone` (nullable). Jackson parses it, so an
  invalid value fails while reading the body (`HttpMessageNotReadableException`,
  400 via the base exception handler).
- `ChangeBookStatusCommand` gains `ZoneId timeZone` (nullable; `java.time` is
  allowed in `application`).
- `BookService.changeStatus` computes today as
  `LocalDate.now(timeZone == null ? clock : clock.withZone(timeZone))` and
  passes it to `Book.changeStatus`, which is unchanged. The server still
  decides what "today" is; the client only says where the user is.
- No migration: `started_at` and `finished_at` are already `DATE`.

## Acceptance criteria

- [ ] At 2026-10-01T01:00Z with `America/Bogota`, moving to `READING` sets
      `startedAt` to 2026-09-30, and moving to `READ` sets `finishedAt` to 2026-09-30.
- [ ] Zones east of UTC get the later date (2026-09-30T23:00Z in
      `Asia/Tokyo` is 2026-10-01).
- [ ] Without `timeZone` (or with `""`), the date comes from the clock's zone (UTC).
- [ ] The controller passes the parsed `timeZone` to the use case.
- [ ] An invalid `timeZone` returns 400 `application/problem+json` and the use
      case is not called.
- [ ] `./gradlew test` passes.
