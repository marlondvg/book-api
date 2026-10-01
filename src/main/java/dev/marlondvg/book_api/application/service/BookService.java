package dev.marlondvg.book_api.application.service;

import dev.marlondvg.book_api.application.port.in.ChangeBookStatusCommand;
import dev.marlondvg.book_api.application.port.in.ChangeBookStatusUseCase;
import dev.marlondvg.book_api.application.port.in.CreateBookCommand;
import dev.marlondvg.book_api.application.port.in.CreateBookUseCase;
import dev.marlondvg.book_api.application.port.in.DeleteBookUseCase;
import dev.marlondvg.book_api.application.port.in.GetBookUseCase;
import dev.marlondvg.book_api.application.port.in.ListBooksUseCase;
import dev.marlondvg.book_api.application.port.in.RateBookCommand;
import dev.marlondvg.book_api.application.port.in.RateBookUseCase;
import dev.marlondvg.book_api.application.port.in.UpdateBookDetailsCommand;
import dev.marlondvg.book_api.application.port.in.UpdateBookDetailsUseCase;
import dev.marlondvg.book_api.application.port.out.BookRepository;
import dev.marlondvg.book_api.domain.Book;
import dev.marlondvg.book_api.domain.Rating;
import dev.marlondvg.book_api.domain.ReadingStatus;
import dev.marlondvg.book_api.domain.exception.BookNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BookService implements CreateBookUseCase, GetBookUseCase, ListBooksUseCase,
		UpdateBookDetailsUseCase, ChangeBookStatusUseCase, RateBookUseCase, DeleteBookUseCase {

	private final BookRepository bookRepository;
	private final Clock clock;

	public BookService(BookRepository bookRepository, Clock clock) {
		this.bookRepository = bookRepository;
		this.clock = clock;
	}

	@Override
	public Book createBook(CreateBookCommand command) {
		Book book = Book.create(command.ownerId(), command.title(), command.author(), command.pages(),
				command.isbn(), command.coverUrl(), Instant.now(clock));
		return bookRepository.save(book);
	}

	@Override
	@Transactional(readOnly = true)
	public Book getBook(UUID ownerId, UUID bookId) {
		return findOwnedBook(ownerId, bookId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Book> listBooks(UUID ownerId, ReadingStatus status) {
		return bookRepository.findAllByOwnerId(ownerId, status);
	}

	@Override
	public Book updateDetails(UpdateBookDetailsCommand command) {
		Book book = findOwnedBook(command.ownerId(), command.bookId());
		book.updateDetails(command.title(), command.author(), command.pages(), command.isbn(),
				command.coverUrl());
		return bookRepository.save(book);
	}

	@Override
	public Book changeStatus(ChangeBookStatusCommand command) {
		Book book = findOwnedBook(command.ownerId(), command.bookId());
		Clock userClock = command.timeZone() == null ? clock : clock.withZone(command.timeZone());
		book.changeStatus(command.status(), LocalDate.now(userClock));
		return bookRepository.save(book);
	}

	@Override
	public Book rate(RateBookCommand command) {
		Rating rating = new Rating(command.rating());
		Book book = findOwnedBook(command.ownerId(), command.bookId());
		book.rate(rating);
		return bookRepository.save(book);
	}

	@Override
	public Book clearRating(UUID ownerId, UUID bookId) {
		Book book = findOwnedBook(ownerId, bookId);
		book.clearRating();
		return bookRepository.save(book);
	}

	@Override
	public void deleteBook(UUID ownerId, UUID bookId) {
		bookRepository.delete(findOwnedBook(ownerId, bookId));
	}

	private Book findOwnedBook(UUID ownerId, UUID bookId) {
		return bookRepository.findByIdAndOwnerId(bookId, ownerId)
				.orElseThrow(() -> new BookNotFoundException(bookId));
	}
}
