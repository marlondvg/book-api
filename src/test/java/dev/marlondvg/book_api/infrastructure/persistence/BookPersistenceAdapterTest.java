package dev.marlondvg.book_api.infrastructure.persistence;

import dev.marlondvg.book_api.domain.Book;
import dev.marlondvg.book_api.domain.Rating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static dev.marlondvg.book_api.domain.ReadingStatus.ABANDONED;
import static dev.marlondvg.book_api.domain.ReadingStatus.READ;
import static dev.marlondvg.book_api.domain.ReadingStatus.READING;
import static org.assertj.core.api.Assertions.assertThat;

// Uses the H2 URL from application.yaml (PostgreSQL mode) instead of a plain embedded database.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BookPersistenceAdapter.class)
class BookPersistenceAdapterTest {

	private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

	@Autowired
	private BookPersistenceAdapter adapter;

	@Autowired
	private TestEntityManager entityManager;

	private UUID ownerId;
	private UUID otherOwnerId;

	@BeforeEach
	void setUp() {
		ownerId = insertUser("owner@example.com");
		otherOwnerId = insertUser("other@example.com");
	}

	private UUID insertUser(String email) {
		UUID id = UUID.randomUUID();
		entityManager.getEntityManager()
				.createNativeQuery("INSERT INTO users (id, email, password_hash, created_at) VALUES (?, ?, ?, ?)")
				.setParameter(1, id)
				.setParameter(2, email)
				.setParameter(3, "not-a-real-hash")
				.setParameter(4, CREATED_AT)
				.executeUpdate();
		return id;
	}

	private Book saveBook(UUID owner, String title, Instant createdAt) {
		Book book = Book.create(owner, title, "Some Author", null, null, null, createdAt);
		return adapter.save(book);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void shouldReadBackSavedBookWithAllFields() {
		Book book = Book.create(ownerId, "Dune", "Frank Herbert", 412, "9780441013593",
				"https://example.com/dune.jpg", CREATED_AT);
		adapter.save(book);
		flushAndClear();

		Book found = adapter.findByIdAndOwnerId(book.getId(), ownerId).orElseThrow();

		assertThat(found.getId()).isEqualTo(book.getId());
		assertThat(found.getOwnerId()).isEqualTo(ownerId);
		assertThat(found.getTitle()).isEqualTo("Dune");
		assertThat(found.getAuthor()).isEqualTo("Frank Herbert");
		assertThat(found.getPages()).isEqualTo(412);
		assertThat(found.getIsbn()).isEqualTo("9780441013593");
		assertThat(found.getCoverUrl()).isEqualTo("https://example.com/dune.jpg");
		assertThat(found.getStatus()).isEqualTo(book.getStatus());
		assertThat(found.getRating()).isNull();
		assertThat(found.getStartedAt()).isNull();
		assertThat(found.getFinishedAt()).isNull();
		assertThat(found.getCreatedAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void shouldPersistChangesToExistingBook() {
		Book book = saveBook(ownerId, "Dune", CREATED_AT);
		flushAndClear();

		book.updateDetails("Dune Messiah", "Frank Herbert", 256, null, null);
		book.changeStatus(READING, LocalDate.of(2026, 2, 1));
		book.changeStatus(READ, LocalDate.of(2026, 3, 1));
		book.rate(new Rating(4));
		adapter.save(book);
		flushAndClear();

		Book found = adapter.findByIdAndOwnerId(book.getId(), ownerId).orElseThrow();
		assertThat(found.getTitle()).isEqualTo("Dune Messiah");
		assertThat(found.getPages()).isEqualTo(256);
		assertThat(found.getStatus()).isEqualTo(READ);
		assertThat(found.getStartedAt()).isEqualTo(LocalDate.of(2026, 2, 1));
		assertThat(found.getFinishedAt()).isEqualTo(LocalDate.of(2026, 3, 1));
		assertThat(found.getRating()).isEqualTo(new Rating(4));
	}

	@Test
	void shouldNotFindBookOwnedByAnotherUser() {
		Book book = saveBook(ownerId, "Dune", CREATED_AT);
		flushAndClear();

		assertThat(adapter.findByIdAndOwnerId(book.getId(), otherOwnerId)).isEmpty();
	}

	@Test
	void shouldListOnlyOwnersBooksNewestFirst() {
		Book oldest = saveBook(ownerId, "Oldest", CREATED_AT);
		Book newest = saveBook(ownerId, "Newest", CREATED_AT.plusSeconds(120));
		Book middle = saveBook(ownerId, "Middle", CREATED_AT.plusSeconds(60));
		saveBook(otherOwnerId, "Not mine", CREATED_AT.plusSeconds(180));
		flushAndClear();

		assertThat(adapter.findAllByOwnerId(ownerId, null))
				.extracting(Book::getId)
				.containsExactly(newest.getId(), middle.getId(), oldest.getId());
	}

	@Test
	void shouldFilterOwnersBooksByStatus() {
		Book reading = saveBook(ownerId, "Reading", CREATED_AT);
		reading.changeStatus(READING, LocalDate.of(2026, 2, 1));
		adapter.save(reading);
		Book abandoned = saveBook(ownerId, "Abandoned", CREATED_AT.plusSeconds(60));
		abandoned.changeStatus(ABANDONED, LocalDate.of(2026, 2, 1));
		adapter.save(abandoned);
		saveBook(ownerId, "To read", CREATED_AT.plusSeconds(120));
		Book othersReading = saveBook(otherOwnerId, "Not mine", CREATED_AT);
		othersReading.changeStatus(READING, LocalDate.of(2026, 2, 1));
		adapter.save(othersReading);
		flushAndClear();

		assertThat(adapter.findAllByOwnerId(ownerId, READING))
				.extracting(Book::getId)
				.containsExactly(reading.getId());
	}

	@Test
	void shouldDeleteBook() {
		Book book = saveBook(ownerId, "Dune", CREATED_AT);
		flushAndClear();

		adapter.delete(book);
		flushAndClear();

		assertThat(adapter.findByIdAndOwnerId(book.getId(), ownerId)).isEmpty();
	}
}
