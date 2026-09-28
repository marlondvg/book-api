package dev.marlondvg.book_api.application.port.in;

import java.util.UUID;

public interface DeleteBookUseCase {

	void deleteBook(UUID ownerId, UUID bookId);
}
