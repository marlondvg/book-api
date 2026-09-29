package dev.marlondvg.book_api.domain.exception;

/**
 * The token is valid but its user no longer exists.
 */
public class UnknownUserException extends DomainException {

	public UnknownUserException() {
		super("User no longer exists");
	}
}
