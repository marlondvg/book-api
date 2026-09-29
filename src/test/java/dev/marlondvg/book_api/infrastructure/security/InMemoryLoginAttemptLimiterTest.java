package dev.marlondvg.book_api.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryLoginAttemptLimiterTest {

	private static final String EMAIL = "ann@example.com";
	private static final String IP = "203.0.113.7";

	private final MutableClock clock = new MutableClock(Instant.parse("2026-04-01T12:00:00Z"));
	private final InMemoryLoginAttemptLimiter limiter =
			new InMemoryLoginAttemptLimiter(new LoginLimitProperties(5, 20, Duration.ofMinutes(15)), clock);

	private void fail(String email, String ip, int times) {
		for (int i = 0; i < times; i++) {
			limiter.recordFailure(email, ip);
		}
	}

	@Test
	void shouldAllowUpToFourFailuresPerEmail() {
		fail(EMAIL, IP, 4);

		assertThat(limiter.retryAfter(EMAIL, IP)).isEmpty();
	}

	@Test
	void shouldBlockEmailAfterFiveFailuresUntilWindowEnds() {
		fail(EMAIL, IP, 5);
		clock.advance(Duration.ofMinutes(5));

		assertThat(limiter.retryAfter(EMAIL, IP)).contains(Duration.ofMinutes(10));
		assertThat(limiter.retryAfter(EMAIL, "198.51.100.1")).contains(Duration.ofMinutes(10));
	}

	@Test
	void shouldAllowAgainOnceWindowHasEnded() {
		fail(EMAIL, IP, 5);
		clock.advance(Duration.ofMinutes(15));

		assertThat(limiter.retryAfter(EMAIL, IP)).isEmpty();
	}

	@Test
	void shouldStartNewWindowAfterExpiry() {
		fail(EMAIL, IP, 4);
		clock.advance(Duration.ofMinutes(16));
		fail(EMAIL, IP, 4);

		assertThat(limiter.retryAfter(EMAIL, IP)).isEmpty();
	}

	@Test
	void shouldNotAffectOtherEmails() {
		fail(EMAIL, IP, 5);

		assertThat(limiter.retryAfter("bob@example.com", "198.51.100.1")).isEmpty();
	}

	@Test
	void shouldBlockIpAfterTwentyFailuresAcrossEmails() {
		for (int i = 0; i < 20; i++) {
			limiter.recordFailure("user" + i + "@example.com", IP);
		}

		assertThat(limiter.retryAfter("new@example.com", IP)).isPresent();
		assertThat(limiter.retryAfter("new@example.com", "198.51.100.1")).isEmpty();
	}

	@Test
	void shouldCountMalformedEmailOnlyAgainstIp() {
		fail(null, IP, 20);

		assertThat(limiter.retryAfter(null, IP)).isPresent();
		assertThat(limiter.retryAfter(EMAIL, "198.51.100.1")).isEmpty();
	}

	@Test
	void shouldReturnLongestWaitWhenEmailAndIpAreBothBlocked() {
		for (int i = 0; i < 20; i++) {
			limiter.recordFailure("user" + i + "@example.com", IP);
		}
		clock.advance(Duration.ofMinutes(10));
		fail(EMAIL, "198.51.100.1", 5);

		assertThat(limiter.retryAfter(EMAIL, IP)).contains(Duration.ofMinutes(15));
	}

	@Test
	void shouldResetEmailCounterOnSuccess() {
		fail(EMAIL, IP, 4);
		limiter.recordSuccess(EMAIL);
		fail(EMAIL, IP, 4);

		assertThat(limiter.retryAfter(EMAIL, IP)).isEmpty();
	}

	@Test
	void shouldPurgeExpiredWindowsWhenManyKeysAreTracked() {
		for (int i = 0; i <= InMemoryLoginAttemptLimiter.PURGE_THRESHOLD; i++) {
			limiter.recordFailure("user" + i + "@example.com", null);
		}
		clock.advance(Duration.ofMinutes(16));

		limiter.recordFailure(EMAIL, IP);

		assertThat(limiter.trackedKeys()).isEqualTo(2);
	}

	private static final class MutableClock extends Clock {

		private Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}
	}
}
