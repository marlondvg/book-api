package dev.marlondvg.book_api.application.port.in;

import java.util.UUID;

public record CreateBookCommand(
		UUID ownerId,
		String title,
		String author,
		Integer pages,
		String isbn,
		String coverUrl) {
}
