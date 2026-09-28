package dev.marlondvg.book_api.domain.exception;

public abstract class DomainException extends RuntimeException {

	protected DomainException(String message) {
		super(message);
	}
}
