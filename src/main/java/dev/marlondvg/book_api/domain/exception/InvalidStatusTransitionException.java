package dev.marlondvg.book_api.domain.exception;

import dev.marlondvg.book_api.domain.ReadingStatus;

public class InvalidStatusTransitionException extends DomainException {

	public InvalidStatusTransitionException(ReadingStatus from, ReadingStatus to) {
		super("Cannot change status from %s to %s".formatted(from, to));
	}
}
