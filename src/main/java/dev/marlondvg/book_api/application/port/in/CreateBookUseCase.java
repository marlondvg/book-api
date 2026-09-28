package dev.marlondvg.book_api.application.port.in;

import dev.marlondvg.book_api.domain.Book;

public interface CreateBookUseCase {

	Book createBook(CreateBookCommand command);
}
