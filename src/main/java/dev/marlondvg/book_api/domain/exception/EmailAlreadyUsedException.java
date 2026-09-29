package dev.marlondvg.book_api.domain.exception;

public class EmailAlreadyUsedException extends DomainException {

	public EmailAlreadyUsedException() {
		super("An account with this email already exists");
	}
}
