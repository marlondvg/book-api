package dev.marlondvg.book_api.domain;

import dev.marlondvg.book_api.domain.exception.InvalidRatingException;

public record Rating(int value) {

	public static final int MIN = 1;
	public static final int MAX = 5;

	public Rating {
		if (value < MIN || value > MAX) {
			throw new InvalidRatingException(value);
		}
	}
}
