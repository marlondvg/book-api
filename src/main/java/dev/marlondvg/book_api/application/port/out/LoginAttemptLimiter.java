package dev.marlondvg.book_api.application.port.out;

import java.time.Duration;
import java.util.Optional;

/**
 * Tracks failed logins per email and per client IP. {@code email} is the
 * normalized email, or {@code null} when the given email was malformed.
 */
public interface LoginAttemptLimiter {

	/**
	 * Returns how long until login is allowed again, or empty if it is allowed now.
	 */
	Optional<Duration> retryAfter(String email, String clientIp);

	void recordFailure(String email, String clientIp);

	void recordSuccess(String email);
}
