package dev.marlondvg.book_api.domain;

import dev.marlondvg.book_api.domain.exception.InvalidUserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

	private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

	@Test
	void shouldRegisterUserWithNormalizedEmail() {
		User user = User.register("  Ann@Example.COM ", "hash", CREATED_AT);

		assertThat(user.getId()).isNotNull();
		assertThat(user.getEmail()).isEqualTo("ann@example.com");
		assertThat(user.getPasswordHash()).isEqualTo("hash");
		assertThat(user.getCreatedAt()).isEqualTo(CREATED_AT);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"   ", "ann", "ann@", "@example.com", "ann@example", "ann smith@example.com", "a@b@c.com"})
	void shouldRejectInvalidEmail(String email) {
		assertThatThrownBy(() -> User.register(email, "hash", CREATED_AT))
				.isInstanceOf(InvalidUserException.class);
	}

	@Test
	void shouldRejectTooLongEmail() {
		String email = "a".repeat(250) + "@example.com";

		assertThatThrownBy(() -> User.register(email, "hash", CREATED_AT))
				.isInstanceOf(InvalidUserException.class);
	}

	@Test
	void shouldRejectMissingPasswordHash() {
		assertThatThrownBy(() -> User.register("ann@example.com", null, CREATED_AT))
				.isInstanceOf(InvalidUserException.class);
	}

	@Test
	void shouldNotExposePasswordHashInToString() {
		User user = User.register("ann@example.com", "secret-hash", CREATED_AT);

		assertThat(user.toString()).doesNotContain("secret-hash").doesNotContain("ann@example.com");
	}
}
