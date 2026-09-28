package dev.marlondvg.book_api.application.port.in;

import dev.marlondvg.book_api.domain.Book;

import java.util.UUID;

public interface RateBookUseCase {

	Book rate(RateBookCommand command);

	Book clearRating(UUID ownerId, UUID bookId);
}
