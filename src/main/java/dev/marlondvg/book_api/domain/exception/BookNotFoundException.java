package dev.marlondvg.book_api.domain.exception;

import java.util.UUID;

public class BookNotFoundException extends DomainException {

	public BookNotFoundException(UUID bookId) {
		super("Book not found: " + bookId);
	}
}
