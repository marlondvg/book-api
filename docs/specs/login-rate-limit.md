# Spec: Login rate limit

## Goal

Slow down password guessing on `POST /api/auth/login` by limiting failed
attempts per account and per client, without adding a dependency.

Out of scope: limits on other endpoints, CAPTCHA, a shared store for several
app instances (Redis), alerts.

## Rules

- Failed logins are counted in fixed windows of `window` (default 15 minutes):
  - per email (normalized): at most `max-failures-per-email` (default 5);
  - per client IP: at most `max-failures-per-ip` (default 20).
- Once either limit is reached, every login for that email or from that IP is
  refused until its window ends, **even with the correct password**, with
  `TooManyLoginAttemptsException` -> **429**, a problem-details body and a
  `Retry-After` header (seconds until the window ends).
- The check happens before any password hashing, so blocked requests cost no
  BCrypt time.
- A successful login resets the counter for that email (not for the IP).
- A malformed email only counts against the IP.
- Settings under `app.login-limit` (`max-failures-per-email`,
  `max-failures-per-ip`, `window`).

## Design

- Application port `LoginAttemptLimiter` (`port/out`):
  `retryAfter(email, clientIp)`, `recordFailure(email, clientIp)`,
  `recordSuccess(email)`. `AuthService.login` uses it; the rule of *when* to
  check and record lives in the service.
- `LoginCommand` gains `clientIp`; the controller reads it from
  `HttpServletRequest.getRemoteAddr()`. In production
  `forward-headers-strategy: framework` fills it from `X-Forwarded-For`.
- `InMemoryLoginAttemptLimiter` (`infrastructure/security`): a
  `ConcurrentHashMap` of fixed windows, using the `Clock` bean. When it holds
  more than 10,000 keys, expired windows are removed.

## Known limits

- In memory: counters reset on restart and are not shared between instances.
  Fine for one Render instance.
- `X-Forwarded-For` can be set by the client, so the IP limit is best effort.
  The per-email limit is the main protection.
- Someone who knows an email can lock that account out of login for up to one
  window by failing on purpose. Accepted for a personal app; the window is short.

## Acceptance criteria

- [ ] The 6th login for an email within the window gets 429 with `Retry-After`,
      even with the right password, and nothing is hashed.
- [ ] After the window ends, login works again.
- [ ] 20 failures from one IP across different emails block that IP.
- [ ] A successful login resets the email counter.
- [ ] Other emails and other IPs are not affected.
- [ ] Limiter unit tests with a controllable clock; service, web and
      end-to-end tests.
- [ ] `./gradlew test` passes.
