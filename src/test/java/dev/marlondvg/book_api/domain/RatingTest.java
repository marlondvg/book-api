package dev.marlondvg.book_api.domain;

import dev.marlondvg.book_api.domain.exception.InvalidRatingException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RatingTest {

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4, 5})
	void shouldAcceptValuesFromOneToFive(int value) {
		assertThat(new Rating(value).value()).isEqualTo(value);
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, 0, 6, 10})
	void shouldRejectValuesOutsideOneToFive(int value) {
		assertThatThrownBy(() -> new Rating(value))
				.isInstanceOf(InvalidRatingException.class);
	}
}
