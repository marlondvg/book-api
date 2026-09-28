package dev.marlondvg.book_api.application.service;

import dev.marlondvg.book_api.application.port.in.ChangeBookStatusCommand;
import dev.marlondvg.book_api.application.port.in.CreateBookCommand;
import dev.marlondvg.book_api.application.port.in.RateBookCommand;
import dev.marlondvg.book_api.application.port.in.UpdateBookDetailsCommand;
import dev.marlondvg.book_api.application.port.out.BookRepository;
import dev.marlondvg.book_api.domain.Book;
import dev.marlondvg.book_api.domain.Rating;
import dev.marlondvg.book_api.domain.ReadingStatus;
import dev.marlondvg.book_api.domain.exception.BookNotFoundException;
import dev.marlondvg.book_api.domain.exception.InvalidBookException;
import dev.marlondvg.book_api.domain.exception.InvalidRatingException;
import dev.marlondvg.book_api.domain.exception.InvalidStatusTransitionException;
import dev.marlondvg.book_api.domain.exception.RatingNotAllowedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dev.marlondvg.book_api.domain.ReadingStatus.READ;
import static dev.marlondvg.book_api.domain.ReadingStatus.READING;
import static dev.marlondvg.book_api.domain.ReadingStatus.TO_READ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

	private static final Instant NOW = Instant.parse("2026-04-01T12:00:00Z");
	private static final LocalDate TODAY = LocalDate.of(2026, 4, 1);
	private static final LocalDate STARTED = LocalDate.of(2026, 3, 1);
	private static final UUID OWNER_ID = UUID.randomUUID();
	private static final UUID OTHER_OWNER_ID = UUID.randomUUID();

	@Mock
	private BookRepository bookRepository;

	private BookService bookService;

	@BeforeEach
	void setUp() {
		bookService = new BookService(bookRepository, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private static Book storedBook(ReadingStatus status) {
		LocalDate startedAt = status == TO_READ ? null : STARTED;
		return Book.restore(UUID.randomUUID(), OWNER_ID, "Dune", "Frank Herbert", 412, null, null,
				status, null, startedAt, null, NOW.minusSeconds(86_400));
	}

	private void givenStored(Book book) {
		when(bookRepository.findByIdAndOwnerId(book.getId(), OWNER_ID)).thenReturn(Optional.of(book));
	}

	private void givenSaveReturnsArgument() {
		when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Nested
	class CreateBook {

		@Test
		void shouldSaveNewToReadBookOwnedByCaller() {
			givenSaveReturnsArgument();

			Book book = bookService.createBook(
					new CreateBookCommand(OWNER_ID, "Dune", "Frank Herbert", 412, null, null));

			assertThat(book.getOwnerId()).isEqualTo(OWNER_ID);
			assertThat(book.getStatus()).isEqualTo(TO_READ);
			assertThat(book.getCreatedAt()).isEqualTo(NOW);
			verify(bookRepository).save(book);
		}

		@Test
		void shouldNotSaveBookWithInvalidDetails() {
			assertThatThrownBy(() -> bookService.createBook(
					new CreateBookCommand(OWNER_ID, " ", "Frank Herbert", null, null, null)))
					.isInstanceOf(InvalidBookException.class);

			verifyNoInteractions(bookRepository);
		}
	}

	@Nested
	class GetBook {

		@Test
		void shouldReturnOwnedBook() {
			Book stored = storedBook(TO_READ);
			givenStored(stored);

			assertThat(bookService.getBook(OWNER_ID, stored.getId())).isSameAs(stored);
		}

		@Test
		void shouldThrowNotFoundWhenBookBelongsToAnotherOwner() {
			UUID bookId = UUID.randomUUID();
			when(bookRepository.findByIdAndOwnerId(bookId, OTHER_OWNER_ID)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> bookService.getBook(OTHER_OWNER_ID, bookId))
					.isInstanceOf(BookNotFoundException.class)
					.hasMessageContaining(bookId.toString());
		}
	}

	@Nested
	class ListBooks {

		@Test
		void shouldPassOwnerAndStatusFilterToRepository() {
			List<Book> books = List.of(storedBook(READING));
			when(bookRepository.findAllByOwnerId(OWNER_ID, READING)).thenReturn(books);

			assertThat(bookService.listBooks(OWNER_ID, READING)).isEqualTo(books);
		}

		@Test
		void shouldListAllStatusesWhenFilterIsNull() {
			when(bookRepository.findAllByOwnerId(OWNER_ID, null)).thenReturn(List.of());

			assertThat(bookService.listBooks(OWNER_ID, null)).isEmpty();
		}
	}

	@Nested
	class UpdateDetails {

		@Test
		void shouldUpdateAndSaveOwnedBook() {
			Book stored = storedBook(TO_READ);
			givenStored(stored);
			givenSaveReturnsArgument();

			Book book = bookService.updateDetails(new UpdateBookDetailsCommand(
					OWNER_ID, stored.getId(), "Dune Messiah", "Frank Herbert", 256, null, null));

			assertThat(book.getTitle()).isEqualTo("Dune Messiah");
			assertThat(book.getPages()).isEqualTo(256);
			verify(bookRepository).save(stored);
		}

		@Test
		void shouldThrowNotFoundAndNotSaveWhenBookIsMissing() {
			UUID bookId = UUID.randomUUID();
			when(bookRepository.findByIdAndOwnerId(bookId, OWNER_ID)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> bookService.updateDetails(new UpdateBookDetailsCommand(
					OWNER_ID, bookId, "Dune", "Frank Herbert", null, null, null)))
					.isInstanceOf(BookNotFoundException.class);

			verify(bookRepository, never()).save(any());
		}
	}

	@Nested
	class ChangeStatus {

		@Test
		void shouldUseTodayFromClock() {
			Book stored = storedBook(TO_READ);
			givenStored(stored);
			givenSaveReturnsArgument();

			Book book = bookService.changeStatus(new ChangeBookStatusCommand(OWNER_ID, stored.getId(), READING));

			assertThat(book.getStatus()).isEqualTo(READING);
			assertThat(book.getStartedAt()).isEqualTo(TODAY);
			verify(bookRepository).save(stored);
		}

		@Test
		void shouldPropagateInvalidTransitionAndNotSave() {
			Book stored = storedBook(READ);
			givenStored(stored);

			assertThatThrownBy(() -> bookService.changeStatus(
					new ChangeBookStatusCommand(OWNER_ID, stored.getId(), TO_READ)))
					.isInstanceOf(InvalidStatusTransitionException.class);

			verify(bookRepository, never()).save(any());
		}

		@Test
		void shouldThrowNotFoundWhenBookBelongsToAnotherOwner() {
			UUID bookId = UUID.randomUUID();
			when(bookRepository.findByIdAndOwnerId(bookId, OTHER_OWNER_ID)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> bookService.changeStatus(
					new ChangeBookStatusCommand(OTHER_OWNER_ID, bookId, READING)))
					.isInstanceOf(BookNotFoundException.class);
		}
	}

	@Nested
	class Rate {

		@Test
		void shouldRateFinishedBook() {
			Book stored = storedBook(READ);
			givenStored(stored);
			givenSaveReturnsArgument();

			Book book = bookService.rate(new RateBookCommand(OWNER_ID, stored.getId(), 5));

			assertThat(book.getRating()).isEqualTo(new Rating(5));
			verify(bookRepository).save(stored);
		}

		@Test
		void shouldRejectInvalidRatingBeforeLoadingBook() {
			assertThatThrownBy(() -> bookService.rate(new RateBookCommand(OWNER_ID, UUID.randomUUID(), 6)))
					.isInstanceOf(InvalidRatingException.class);

			verifyNoInteractions(bookRepository);
		}

		@Test
		void shouldPropagateRatingNotAllowedAndNotSave() {
			Book stored = storedBook(READING);
			givenStored(stored);

			assertThatThrownBy(() -> bookService.rate(new RateBookCommand(OWNER_ID, stored.getId(), 4)))
					.isInstanceOf(RatingNotAllowedException.class);

			verify(bookRepository, never()).save(any());
		}

		@Test
		void shouldClearRating() {
			Book stored = storedBook(READ);
			stored.rate(new Rating(3));
			givenStored(stored);
			givenSaveReturnsArgument();

			Book book = bookService.clearRating(OWNER_ID, stored.getId());

			assertThat(book.getRating()).isNull();
			verify(bookRepository).save(stored);
		}

		@Test
		void shouldThrowNotFoundWhenClearingRatingOfMissingBook() {
			UUID bookId = UUID.randomUUID();
			when(bookRepository.findByIdAndOwnerId(bookId, OWNER_ID)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> bookService.clearRating(OWNER_ID, bookId))
					.isInstanceOf(BookNotFoundException.class);
		}
	}

	@Nested
	class DeleteBook {

		@Test
		void shouldDeleteOwnedBook() {
			Book stored = storedBook(TO_READ);
			givenStored(stored);

			bookService.deleteBook(OWNER_ID, stored.getId());

			verify(bookRepository).delete(stored);
		}

		@Test
		void shouldThrowNotFoundAndNotDeleteWhenBookBelongsToAnotherOwner() {
			UUID bookId = UUID.randomUUID();
			when(bookRepository.findByIdAndOwnerId(bookId, OTHER_OWNER_ID)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> bookService.deleteBook(OTHER_OWNER_ID, bookId))
					.isInstanceOf(BookNotFoundException.class);

			verify(bookRepository, never()).delete(any());
		}
	}
}
