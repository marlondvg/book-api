package dev.marlondvg.book_api.infrastructure.security;

import dev.marlondvg.book_api.application.port.out.LoginAttemptLimiter;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

/**
 * Counts failed logins in fixed windows, in memory. Counters are lost on restart
 * and not shared between instances, which is fine for a single instance.
 */
@Component
class InMemoryLoginAttemptLimiter implements LoginAttemptLimiter {

	// Above this many keys, expired windows are removed so memory stays bounded.
	static final int PURGE_THRESHOLD = 10_000;

	private record Window(Instant start, int failures) {
	}

	private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
	private final LoginLimitProperties properties;
	private final Clock clock;

	InMemoryLoginAttemptLimiter(LoginLimitProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	public Optional<Duration> retryAfter(String email, String clientIp) {
		Instant now = clock.instant();
		return Stream.of(
						blockedFor(emailKey(email), properties.maxFailuresPerEmail(), now),
						blockedFor(ipKey(clientIp), properties.maxFailuresPerIp(), now))
				.flatMap(Optional::stream)
				.max(Duration::compareTo);
	}

	@Override
	public void recordFailure(String email, String clientIp) {
		Instant now = clock.instant();
		increment(emailKey(email), now);
		increment(ipKey(clientIp), now);
		if (windows.size() > PURGE_THRESHOLD) {
			windows.values().removeIf(window -> isExpired(window, now));
		}
	}

	@Override
	public void recordSuccess(String email) {
		String key = emailKey(email);
		if (key != null) {
			windows.remove(key);
		}
	}

	int trackedKeys() {
		return windows.size();
	}

	private Optional<Duration> blockedFor(String key, int maxFailures, Instant now) {
		if (key == null) {
			return Optional.empty();
		}
		Window window = windows.get(key);
		if (window == null || isExpired(window, now) || window.failures() < maxFailures) {
			return Optional.empty();
		}
		return Optional.of(Duration.between(now, window.start().plus(properties.window())));
	}

	private void increment(String key, Instant now) {
		if (key == null) {
			return;
		}
		windows.compute(key, (k, window) -> window == null || isExpired(window, now)
				? new Window(now, 1)
				: new Window(window.start(), window.failures() + 1));
	}

	private boolean isExpired(Window window, Instant now) {
		return !now.isBefore(window.start().plus(properties.window()));
	}

	private static String emailKey(String email) {
		return email == null ? null : "email:" + email;
	}

	private static String ipKey(String clientIp) {
		return clientIp == null || clientIp.isBlank() ? null : "ip:" + clientIp;
	}
}
