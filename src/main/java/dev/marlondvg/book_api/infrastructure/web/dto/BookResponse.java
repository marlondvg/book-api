package dev.marlondvg.book_api.infrastructure.web.dto;

import dev.marlondvg.book_api.domain.Book;
import dev.marlondvg.book_api.domain.ReadingStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BookResponse(
		UUID id,
		String title,
		String author,
		Integer pages,
		String isbn,
		String coverUrl,
		ReadingStatus status,
		Integer rating,
		LocalDate startedAt,
		LocalDate finishedAt,
		Instant createdAt) {

	public static BookResponse from(Book book) {
		Integer rating = book.getRating() == null ? null : book.getRating().value();
		return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getPages(),
				book.getIsbn(), book.getCoverUrl(), book.getStatus(), rating, book.getStartedAt(),
				book.getFinishedAt(), book.getCreatedAt());
	}
}
