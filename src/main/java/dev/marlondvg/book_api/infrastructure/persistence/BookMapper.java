package dev.marlondvg.book_api.infrastructure.persistence;

import dev.marlondvg.book_api.domain.Book;
import dev.marlondvg.book_api.domain.Rating;

final class BookMapper {

	private BookMapper() {
	}

	static BookJpaEntity toEntity(Book book) {
		Short rating = book.getRating() == null ? null : (short) book.getRating().value();
		return new BookJpaEntity(book.getId(), book.getOwnerId(), book.getTitle(), book.getAuthor(),
				book.getPages(), book.getIsbn(), book.getCoverUrl(), book.getStatus(), rating,
				book.getStartedAt(), book.getFinishedAt(), book.getCreatedAt());
	}

	static Book toDomain(BookJpaEntity entity) {
		Rating rating = entity.getRating() == null ? null : new Rating(entity.getRating());
		return Book.restore(entity.getId(), entity.getOwnerId(), entity.getTitle(), entity.getAuthor(),
				entity.getPages(), entity.getIsbn(), entity.getCoverUrl(), entity.getStatus(), rating,
				entity.getStartedAt(), entity.getFinishedAt(), entity.getCreatedAt());
	}
}
