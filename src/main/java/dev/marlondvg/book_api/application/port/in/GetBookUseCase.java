package dev.marlondvg.book_api.application.port.in;

import dev.marlondvg.book_api.domain.Book;

import java.util.UUID;

public interface GetBookUseCase {

	Book getBook(UUID ownerId, UUID bookId);
}
