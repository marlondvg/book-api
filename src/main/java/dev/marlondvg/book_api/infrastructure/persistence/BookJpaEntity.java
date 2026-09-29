package dev.marlondvg.book_api.infrastructure.persistence;

import dev.marlondvg.book_api.domain.ReadingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "books")
public class BookJpaEntity {

	@Id
	private UUID id;

	@Column(name = "owner_id", nullable = false, updatable = false)
	private UUID ownerId;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String author;

	private Integer pages;

	@Column(length = 20)
	private String isbn;

	@Column(name = "cover_url", length = 500)
	private String coverUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReadingStatus status;

	private Short rating;

	@Column(name = "started_at")
	private LocalDate startedAt;

	@Column(name = "finished_at")
	private LocalDate finishedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected BookJpaEntity() {
	}

	BookJpaEntity(UUID id, UUID ownerId, String title, String author, Integer pages, String isbn,
			String coverUrl, ReadingStatus status, Short rating, LocalDate startedAt, LocalDate finishedAt,
			Instant createdAt) {
		this.id = id;
		this.ownerId = ownerId;
		this.title = title;
		this.author = author;
		this.pages = pages;
		this.isbn = isbn;
		this.coverUrl = coverUrl;
		this.status = status;
		this.rating = rating;
		this.startedAt = startedAt;
		this.finishedAt = finishedAt;
		this.createdAt = createdAt;
	}

	UUID getId() {
		return id;
	}

	UUID getOwnerId() {
		return ownerId;
	}

	String getTitle() {
		return title;
	}

	String getAuthor() {
		return author;
	}

	Integer getPages() {
		return pages;
	}

	String getIsbn() {
		return isbn;
	}

	String getCoverUrl() {
		return coverUrl;
	}

	ReadingStatus getStatus() {
		return status;
	}

	Short getRating() {
		return rating;
	}

	LocalDate getStartedAt() {
		return startedAt;
	}

	LocalDate getFinishedAt() {
		return finishedAt;
	}

	Instant getCreatedAt() {
		return createdAt;
	}
}
