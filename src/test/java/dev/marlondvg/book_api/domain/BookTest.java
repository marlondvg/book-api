package dev.marlondvg.book_api.domain;

import dev.marlondvg.book_api.domain.exception.InvalidBookException;
import dev.marlondvg.book_api.domain.exception.InvalidStatusTransitionException;
import dev.marlondvg.book_api.domain.exception.RatingNotAllowedException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static dev.marlondvg.book_api.domain.ReadingStatus.ABANDONED;
import static dev.marlondvg.book_api.domain.ReadingStatus.READ;
import static dev.marlondvg.book_api.domain.ReadingStatus.READING;
import static dev.marlondvg.book_api.domain.ReadingStatus.TO_READ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookTest {

	private static final UUID OWNER_ID = UUID.randomUUID();
	private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
	private static final LocalDate STARTED = LocalDate.of(2026, 2, 1);
	private static final LocalDate FINISHED = LocalDate.of(2026, 3, 1);
	private static final LocalDate TODAY = LocalDate.of(2026, 4, 1);

	private static Book newBook() {
		return Book.create(OWNER_ID, "Dune", "Frank Herbert", 412, "9780441013593", null, CREATED_AT);
	}

	private static Book bookIn(ReadingStatus status) {
		LocalDate startedAt = status == TO_READ ? null : STARTED;
		LocalDate finishedAt = status == READ ? FINISHED : null;
		Rating rating = status.allowsRating() ? new Rating(4) : null;
		return Book.restore(UUID.randomUUID(), OWNER_ID, "Dune", "Frank Herbert", 412, null, null,
				status, rating, startedAt, finishedAt, CREATED_AT);
	}

	@Nested
	class Creation {

		@Test
		void shouldStartAsToReadWithoutRatingOrDates() {
			Book book = newBook();

			assertThat(book.getId()).isNotNull();
			assertThat(book.getOwnerId()).isEqualTo(OWNER_ID);
			assertThat(book.getStatus()).isEqualTo(TO_READ);
			assertThat(book.getRating()).isNull();
			assertThat(book.getStartedAt()).isNull();
			assertThat(book.getFinishedAt()).isNull();
			assertThat(book.getCreatedAt()).isEqualTo(CREATED_AT);
		}

		@Test
		void shouldTrimTextAndStoreBlankOptionalFieldsAsNull() {
			Book book = Book.create(OWNER_ID, "  Dune ", " Frank Herbert ", null, "  ", "", CREATED_AT);

			assertThat(book.getTitle()).isEqualTo("Dune");
			assertThat(book.getAuthor()).isEqualTo("Frank Herbert");
			assertThat(book.getIsbn()).isNull();
			assertThat(book.getCoverUrl()).isNull();
		}

		@ParameterizedTest
		@NullAndEmptySource
		@ValueSource(strings = "   ")
		void shouldRejectMissingTitle(String title) {
			assertThatThrownBy(() -> Book.create(OWNER_ID, title, "Frank Herbert", null, null, null, CREATED_AT))
					.isInstanceOf(InvalidBookException.class)
					.hasMessageContaining("title");
		}

		@ParameterizedTest
		@NullAndEmptySource
		@ValueSource(strings = "   ")
		void shouldRejectMissingAuthor(String author) {
			assertThatThrownBy(() -> Book.create(OWNER_ID, "Dune", author, null, null, null, CREATED_AT))
					.isInstanceOf(InvalidBookException.class)
					.hasMessageContaining("author");
		}

		@ParameterizedTest
		@ValueSource(ints = {0, -5})
		void shouldRejectNonPositivePages(int pages) {
			assertThatThrownBy(() -> Book.create(OWNER_ID, "Dune", "Frank Herbert", pages, null, null, CREATED_AT))
					.isInstanceOf(InvalidBookException.class)
					.hasMessageContaining("pages");
		}

		@Test
		void shouldRejectTooLongTitle() {
			String title = "a".repeat(256);

			assertThatThrownBy(() -> Book.create(OWNER_ID, title, "Frank Herbert", null, null, null, CREATED_AT))
					.isInstanceOf(InvalidBookException.class);
		}

		@Test
		void shouldRejectTooLongIsbn() {
			String isbn = "1".repeat(21);

			assertThatThrownBy(() -> Book.create(OWNER_ID, "Dune", "Frank Herbert", null, isbn, null, CREATED_AT))
					.isInstanceOf(InvalidBookException.class);
		}

		@Test
		void shouldRejectMissingOwner() {
			assertThatThrownBy(() -> Book.create(null, "Dune", "Frank Herbert", null, null, null, CREATED_AT))
					.isInstanceOf(InvalidBookException.class);
		}
	}

	@Nested
	class Restore {

		@Test
		void shouldRejectRatingOnBookThatIsNotFinished() {
			assertThatThrownBy(() -> Book.restore(UUID.randomUUID(), OWNER_ID, "Dune", "Frank Herbert", null,
					null, null, READING, new Rating(3), STARTED, null, CREATED_AT))
					.isInstanceOf(RatingNotAllowedException.class);
		}
	}

	@Nested
	class UpdateDetails {

		@Test
		void shouldReplaceDetails() {
			Book book = newBook();

			book.updateDetails("Dune Messiah", "Frank Herbert", 256, null, "https://example.com/cover.jpg");

			assertThat(book.getTitle()).isEqualTo("Dune Messiah");
			assertThat(book.getPages()).isEqualTo(256);
			assertThat(book.getIsbn()).isNull();
			assertThat(book.getCoverUrl()).isEqualTo("https://example.com/cover.jpg");
		}

		@Test
		void shouldKeepPreviousDetailsWhenUpdateIsInvalid() {
			Book book = newBook();

			assertThatThrownBy(() -> book.updateDetails("Dune Messiah", " ", 256, null, null))
					.isInstanceOf(InvalidBookException.class);

			assertThat(book.getTitle()).isEqualTo("Dune");
			assertThat(book.getPages()).isEqualTo(412);
		}
	}

	@Nested
	class StatusTransitions {

		@ParameterizedTest(name = "{0} -> {1}")
		@CsvSource({
				"TO_READ, READING",
				"TO_READ, READ",
				"TO_READ, ABANDONED",
				"READING, TO_READ",
				"READING, READ",
				"READING, ABANDONED",
				"READ, READING",
				"ABANDONED, TO_READ",
				"ABANDONED, READING"
		})
		void shouldAllowTransition(ReadingStatus from, ReadingStatus to) {
			Book book = bookIn(from);

			book.changeStatus(to, TODAY);

			assertThat(book.getStatus()).isEqualTo(to);
		}

		@ParameterizedTest(name = "{0} -> {1}")
		@CsvSource({
				"READ, TO_READ",
				"READ, ABANDONED",
				"ABANDONED, READ"
		})
		void shouldRejectTransition(ReadingStatus from, ReadingStatus to) {
			Book book = bookIn(from);

			assertThatThrownBy(() -> book.changeStatus(to, TODAY))
					.isInstanceOf(InvalidStatusTransitionException.class)
					.hasMessageContaining(from.name())
					.hasMessageContaining(to.name());
			assertThat(book.getStatus()).isEqualTo(from);
		}

		@ParameterizedTest
		@EnumSource(ReadingStatus.class)
		void shouldLeaveBookUnchangedWhenStatusIsTheSame(ReadingStatus status) {
			Book book = bookIn(status);
			Rating rating = book.getRating();
			LocalDate startedAt = book.getStartedAt();
			LocalDate finishedAt = book.getFinishedAt();

			book.changeStatus(status, TODAY);

			assertThat(book.getStatus()).isEqualTo(status);
			assertThat(book.getRating()).isEqualTo(rating);
			assertThat(book.getStartedAt()).isEqualTo(startedAt);
			assertThat(book.getFinishedAt()).isEqualTo(finishedAt);
		}

		@Test
		void shouldSetStartedAtWhenMovingToReadingForTheFirstTime() {
			Book book = bookIn(TO_READ);

			book.changeStatus(READING, TODAY);

			assertThat(book.getStartedAt()).isEqualTo(TODAY);
			assertThat(book.getFinishedAt()).isNull();
		}

		@Test
		void shouldKeepStartedAtAndClearFinishedAtAndRatingWhenRereading() {
			Book book = bookIn(READ);

			book.changeStatus(READING, TODAY);

			assertThat(book.getStartedAt()).isEqualTo(STARTED);
			assertThat(book.getFinishedAt()).isNull();
			assertThat(book.getRating()).isNull();
		}

		@Test
		void shouldClearRatingWhenResumingAbandonedBook() {
			Book book = bookIn(ABANDONED);

			book.changeStatus(READING, TODAY);

			assertThat(book.getStartedAt()).isEqualTo(STARTED);
			assertThat(book.getRating()).isNull();
		}

		@Test
		void shouldSetFinishedAtWhenMovingToRead() {
			Book book = bookIn(READING);

			book.changeStatus(READ, TODAY);

			assertThat(book.getFinishedAt()).isEqualTo(TODAY);
			assertThat(book.getStartedAt()).isEqualTo(STARTED);
		}

		@Test
		void shouldNotSetStartedAtWhenMovingFromToReadDirectlyToRead() {
			Book book = bookIn(TO_READ);

			book.changeStatus(READ, TODAY);

			assertThat(book.getStartedAt()).isNull();
			assertThat(book.getFinishedAt()).isEqualTo(TODAY);
		}

		@Test
		void shouldKeepDatesWhenMovingToAbandoned() {
			Book book = bookIn(READING);

			book.changeStatus(ABANDONED, TODAY);

			assertThat(book.getStartedAt()).isEqualTo(STARTED);
			assertThat(book.getFinishedAt()).isNull();
		}

		@ParameterizedTest
		@EnumSource(value = ReadingStatus.class, names = {"READING", "ABANDONED"})
		void shouldClearDatesAndRatingWhenMovingBackToToRead(ReadingStatus from) {
			Book book = bookIn(from);

			book.changeStatus(TO_READ, TODAY);

			assertThat(book.getStartedAt()).isNull();
			assertThat(book.getFinishedAt()).isNull();
			assertThat(book.getRating()).isNull();
		}

		@Test
		void shouldRejectMissingTargetStatus() {
			Book book = newBook();

			assertThatThrownBy(() -> book.changeStatus(null, TODAY))
					.isInstanceOf(InvalidBookException.class);
		}
	}

	@Nested
	class Rating_ {

		@ParameterizedTest
		@EnumSource(value = ReadingStatus.class, names = {"TO_READ", "READING"})
		void shouldRejectRatingWhenBookIsNotFinished(ReadingStatus status) {
			Book book = bookIn(status);

			assertThatThrownBy(() -> book.rate(new Rating(5)))
					.isInstanceOf(RatingNotAllowedException.class);
			assertThat(book.getRating()).isNull();
		}

		@ParameterizedTest
		@EnumSource(value = ReadingStatus.class, names = {"READ", "ABANDONED"})
		void shouldAcceptRatingWhenBookIsReadOrAbandoned(ReadingStatus status) {
			Book book = bookIn(status);

			book.rate(new Rating(5));

			assertThat(book.getRating()).isEqualTo(new Rating(5));
		}

		@Test
		void shouldClearRating() {
			Book book = bookIn(READ);

			book.clearRating();

			assertThat(book.getRating()).isNull();
		}
	}
}
