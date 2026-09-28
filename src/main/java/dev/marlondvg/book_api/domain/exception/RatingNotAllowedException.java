package dev.marlondvg.book_api.domain.exception;

import dev.marlondvg.book_api.domain.ReadingStatus;

public class RatingNotAllowedException extends DomainException {

	public RatingNotAllowedException(ReadingStatus status) {
		super("A book can only be rated when it is READ or ABANDONED, current status is " + status);
	}
}
