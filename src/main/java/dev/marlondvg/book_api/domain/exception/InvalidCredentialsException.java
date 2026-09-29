package dev.marlondvg.book_api.domain.exception;

public class InvalidCredentialsException extends DomainException {

	public InvalidCredentialsException() {
		super("Invalid email or password");
	}
}
