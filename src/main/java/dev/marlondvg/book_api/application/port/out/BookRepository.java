package dev.marlondvg.book_api.application.port.out;

import dev.marlondvg.book_api.domain.Book;
import dev.marlondvg.book_api.domain.ReadingStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookRepository {

	Book save(Book book);

	Optional<Book> findByIdAndOwnerId(UUID id, UUID ownerId);

	/**
	 * Returns the owner's books ordered by creation date, newest first.
	 * A {@code null} status means no status filter.
	 */
	List<Book> findAllByOwnerId(UUID ownerId, ReadingStatus status);

	void delete(Book book);
}
