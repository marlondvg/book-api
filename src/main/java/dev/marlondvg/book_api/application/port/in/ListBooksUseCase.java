package dev.marlondvg.book_api.application.port.in;

import dev.marlondvg.book_api.domain.Book;
import dev.marlondvg.book_api.domain.ReadingStatus;

import java.util.List;
import java.util.UUID;

public interface ListBooksUseCase {

	/**
	 * Lists the owner's books, newest first. A {@code null} status returns all of them.
	 */
	List<Book> listBooks(UUID ownerId, ReadingStatus status);
}
