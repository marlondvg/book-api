package dev.marlondvg.book_api.domain;

import dev.marlondvg.book_api.domain.exception.InvalidPasswordException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"short", "1234567"})
	void shouldRejectPasswordShorterThanEightCharacters(String password) {
		assertThatThrownBy(() -> PasswordPolicy.validate(password))
				.isInstanceOf(InvalidPasswordException.class);
	}

	@Test
	void shouldAcceptPasswordsFromEightCharactersUpTo72Bytes() {
		assertThatCode(() -> PasswordPolicy.validate("12345678")).doesNotThrowAnyException();
		assertThatCode(() -> PasswordPolicy.validate("a".repeat(72))).doesNotThrowAnyException();
	}

	@Test
	void shouldRejectPasswordLongerThan72Bytes() {
		assertThatThrownBy(() -> PasswordPolicy.validate("a".repeat(73)))
				.isInstanceOf(InvalidPasswordException.class);
	}

	@Test
	void shouldCountBytesNotCharacters() {
		// 25 characters, 3 bytes each in UTF-8 = 75 bytes
		String password = "€".repeat(25);

		assertThatThrownBy(() -> PasswordPolicy.validate(password))
				.isInstanceOf(InvalidPasswordException.class);
	}
}
