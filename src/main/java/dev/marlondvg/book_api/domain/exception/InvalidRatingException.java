package dev.marlondvg.book_api.domain.exception;

import dev.marlondvg.book_api.domain.Rating;

public class InvalidRatingException extends DomainException {

	public InvalidRatingException(int value) {
		super("Rating must be between %d and %d, got %d".formatted(Rating.MIN, Rating.MAX, value));
	}
}
