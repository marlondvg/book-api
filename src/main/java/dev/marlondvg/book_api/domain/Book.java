package dev.marlondvg.book_api.domain;

import dev.marlondvg.book_api.domain.exception.InvalidBookException;
import dev.marlondvg.book_api.domain.exception.InvalidStatusTransitionException;
import dev.marlondvg.book_api.domain.exception.RatingNotAllowedException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Book {

	private static final int MAX_TITLE_LENGTH = 255;
	private static final int MAX_AUTHOR_LENGTH = 255;
	private static final int MAX_ISBN_LENGTH = 20;
	private static final int MAX_COVER_URL_LENGTH = 500;

	private final UUID id;
	private final UUID ownerId;
	private final Instant createdAt;
	private String title;
	private String author;
	private Integer pages;
	private String isbn;
	private String coverUrl;
	private ReadingStatus status;
	private Rating rating;
	private LocalDate startedAt;
	private LocalDate finishedAt;

	private Book(UUID id, UUID ownerId, Instant createdAt, ReadingStatus status, Rating rating,
			LocalDate startedAt, LocalDate finishedAt) {
		this.id = requireNonNull(id, "id");
		this.ownerId = requireNonNull(ownerId, "ownerId");
		this.createdAt = requireNonNull(createdAt, "createdAt");
		this.status = requireNonNull(status, "status");
		if (rating != null && !status.allowsRating()) {
			throw new RatingNotAllowedException(status);
		}
		this.rating = rating;
		this.startedAt = startedAt;
		this.finishedAt = finishedAt;
	}

	public static Book create(UUID ownerId, String title, String author, Integer pages, String isbn,
			String coverUrl, Instant createdAt) {
		Book book = new Book(UUID.randomUUID(), ownerId, createdAt, ReadingStatus.TO_READ, null, null, null);
		book.updateDetails(title, author, pages, isbn, coverUrl);
		return book;
	}

	/**
	 * Rebuilds a book from storage, checking the same invariants as a new book.
	 */
	public static Book restore(UUID id, UUID ownerId, String title, String author, Integer pages, String isbn,
			String coverUrl, ReadingStatus status, Rating rating, LocalDate startedAt, LocalDate finishedAt,
			Instant createdAt) {
		Book book = new Book(id, ownerId, createdAt, status, rating, startedAt, finishedAt);
		book.updateDetails(title, author, pages, isbn, coverUrl);
		return book;
	}

	public void updateDetails(String title, String author, Integer pages, String isbn, String coverUrl) {
		String newTitle = requireText(title, "title", MAX_TITLE_LENGTH);
		String newAuthor = requireText(author, "author", MAX_AUTHOR_LENGTH);
		if (pages != null && pages <= 0) {
			throw new InvalidBookException("pages must be greater than 0");
		}
		String newIsbn = optionalText(isbn, "isbn", MAX_ISBN_LENGTH);
		String newCoverUrl = optionalText(coverUrl, "coverUrl", MAX_COVER_URL_LENGTH);

		this.title = newTitle;
		this.author = newAuthor;
		this.pages = pages;
		this.isbn = newIsbn;
		this.coverUrl = newCoverUrl;
	}

	public void changeStatus(ReadingStatus target, LocalDate today) {
		requireNonNull(target, "status");
		requireNonNull(today, "today");
		if (target == status) {
			return;
		}
		if (!status.canTransitionTo(target)) {
			throw new InvalidStatusTransitionException(status, target);
		}

		switch (target) {
			case TO_READ -> {
				startedAt = null;
				finishedAt = null;
			}
			case READING -> {
				if (startedAt == null) {
					startedAt = today;
				}
				finishedAt = null;
			}
			case READ -> finishedAt = today;
			case ABANDONED -> {
			}
		}
		if (!target.allowsRating()) {
			rating = null;
		}
		status = target;
	}

	public void rate(Rating rating) {
		requireNonNull(rating, "rating");
		if (!status.allowsRating()) {
			throw new RatingNotAllowedException(status);
		}
		this.rating = rating;
	}

	public void clearRating() {
		this.rating = null;
	}

	private static <T> T requireNonNull(T value, String field) {
		if (value == null) {
			throw new InvalidBookException(field + " is required");
		}
		return value;
	}

	private static String requireText(String value, String field, int maxLength) {
		String text = optionalText(value, field, maxLength);
		if (text == null) {
			throw new InvalidBookException(field + " is required");
		}
		return text;
	}

	private static String optionalText(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String text = value.strip();
		if (text.length() > maxLength) {
			throw new InvalidBookException(field + " must be at most " + maxLength + " characters");
		}
		return text;
	}

	public UUID getId() {
		return id;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}

	public Integer getPages() {
		return pages;
	}

	public String getIsbn() {
		return isbn;
	}

	public String getCoverUrl() {
		return coverUrl;
	}

	public ReadingStatus getStatus() {
		return status;
	}

	public Rating getRating() {
		return rating;
	}

	public LocalDate getStartedAt() {
		return startedAt;
	}

	public LocalDate getFinishedAt() {
		return finishedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Book other && id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
