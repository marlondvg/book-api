package dev.marlondvg.book_api.domain;

import dev.marlondvg.book_api.domain.exception.InvalidPasswordException;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

	public static final int MIN_LENGTH = 8;
	// BCrypt only uses the first 72 bytes of a password.
	public static final int MAX_BYTES = 72;

	private PasswordPolicy() {
	}

	public static void validate(String rawPassword) {
		if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
			throw new InvalidPasswordException("password must be at least " + MIN_LENGTH + " characters");
		}
		if (exceedsMaxBytes(rawPassword)) {
			throw new InvalidPasswordException("password must be at most " + MAX_BYTES + " bytes");
		}
	}

	public static boolean exceedsMaxBytes(String rawPassword) {
		return rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES;
	}
}
