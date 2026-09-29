package dev.marlondvg.book_api.domain.exception;

public class InvalidPasswordException extends DomainException {

	public InvalidPasswordException(String message) {
		super(message);
	}
}
