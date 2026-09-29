package dev.marlondvg.book_api.domain;

import dev.marlondvg.book_api.domain.exception.InvalidUserException;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public final class User {

	private static final int MAX_EMAIL_LENGTH = 255;
	// Deliberately loose: one "@", no spaces, and a dot in the domain part.
	private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	private final UUID id;
	private final String email;
	private final String passwordHash;
	private final Instant createdAt;

	private User(UUID id, String email, String passwordHash, Instant createdAt) {
		this.id = requireNonNull(id, "id");
		this.email = normalizeEmail(email);
		this.passwordHash = requireNonNull(passwordHash, "passwordHash");
		this.createdAt = requireNonNull(createdAt, "createdAt");
	}

	public static User register(String email, String passwordHash, Instant createdAt) {
		return new User(UUID.randomUUID(), email, passwordHash, createdAt);
	}

	public static User restore(UUID id, String email, String passwordHash, Instant createdAt) {
		return new User(id, email, passwordHash, createdAt);
	}

	/**
	 * Trims and lower-cases an email so the same address always maps to one account.
	 */
	public static String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new InvalidUserException("email is required");
		}
		String normalized = email.strip().toLowerCase(Locale.ROOT);
		if (normalized.length() > MAX_EMAIL_LENGTH) {
			throw new InvalidUserException("email must be at most " + MAX_EMAIL_LENGTH + " characters");
		}
		if (!EMAIL.matcher(normalized).matches()) {
			throw new InvalidUserException("email is not valid");
		}
		return normalized;
	}

	private static <T> T requireNonNull(T value, String field) {
		if (value == null) {
			throw new InvalidUserException(field + " is required");
		}
		return value;
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof User other && id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "User[id=" + id + "]";
	}
}
