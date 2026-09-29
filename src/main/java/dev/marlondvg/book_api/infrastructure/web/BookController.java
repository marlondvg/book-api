package dev.marlondvg.book_api.infrastructure.web;

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
import dev.marlondvg.book_api.domain.Book;
import dev.marlondvg.book_api.domain.ReadingStatus;
import dev.marlondvg.book_api.infrastructure.security.CurrentUserId;
import dev.marlondvg.book_api.infrastructure.web.dto.BookDetailsRequest;
import dev.marlondvg.book_api.infrastructure.web.dto.BookResponse;
import dev.marlondvg.book_api.infrastructure.web.dto.RatingRequest;
import dev.marlondvg.book_api.infrastructure.web.dto.StatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/books")
class BookController {

	private final CreateBookUseCase createBook;
	private final GetBookUseCase getBook;
	private final ListBooksUseCase listBooks;
	private final UpdateBookDetailsUseCase updateBookDetails;
	private final ChangeBookStatusUseCase changeBookStatus;
	private final RateBookUseCase rateBook;
	private final DeleteBookUseCase deleteBook;

	BookController(CreateBookUseCase createBook, GetBookUseCase getBook, ListBooksUseCase listBooks,
			UpdateBookDetailsUseCase updateBookDetails, ChangeBookStatusUseCase changeBookStatus,
			RateBookUseCase rateBook, DeleteBookUseCase deleteBook) {
		this.createBook = createBook;
		this.getBook = getBook;
		this.listBooks = listBooks;
		this.updateBookDetails = updateBookDetails;
		this.changeBookStatus = changeBookStatus;
		this.rateBook = rateBook;
		this.deleteBook = deleteBook;
	}

	@PostMapping
	ResponseEntity<BookResponse> create(@CurrentUserId UUID ownerId, @Valid @RequestBody BookDetailsRequest request) {
		Book book = createBook.createBook(new CreateBookCommand(ownerId, request.title(), request.author(),
				request.pages(), request.isbn(), request.coverUrl()));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(book.getId())
				.toUri();
		return ResponseEntity.created(location).body(BookResponse.from(book));
	}

	@GetMapping
	List<BookResponse> list(@CurrentUserId UUID ownerId, @RequestParam(required = false) ReadingStatus status) {
		return listBooks.listBooks(ownerId, status).stream().map(BookResponse::from).toList();
	}

	@GetMapping("/{id}")
	BookResponse get(@CurrentUserId UUID ownerId, @PathVariable UUID id) {
		return BookResponse.from(getBook.getBook(ownerId, id));
	}

	@PutMapping("/{id}")
	BookResponse update(@CurrentUserId UUID ownerId, @PathVariable UUID id,
			@Valid @RequestBody BookDetailsRequest request) {
		return BookResponse.from(updateBookDetails.updateDetails(new UpdateBookDetailsCommand(ownerId, id,
				request.title(), request.author(), request.pages(), request.isbn(), request.coverUrl())));
	}

	@PutMapping("/{id}/status")
	BookResponse changeStatus(@CurrentUserId UUID ownerId, @PathVariable UUID id,
			@Valid @RequestBody StatusRequest request) {
		return BookResponse.from(changeBookStatus.changeStatus(
				new ChangeBookStatusCommand(ownerId, id, request.status())));
	}

	@PutMapping("/{id}/rating")
	BookResponse rate(@CurrentUserId UUID ownerId, @PathVariable UUID id,
			@Valid @RequestBody RatingRequest request) {
		return BookResponse.from(rateBook.rate(new RateBookCommand(ownerId, id, request.value())));
	}

	@DeleteMapping("/{id}/rating")
	BookResponse clearRating(@CurrentUserId UUID ownerId, @PathVariable UUID id) {
		return BookResponse.from(rateBook.clearRating(ownerId, id));
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@CurrentUserId UUID ownerId, @PathVariable UUID id) {
		deleteBook.deleteBook(ownerId, id);
		return ResponseEntity.noContent().build();
	}
}
