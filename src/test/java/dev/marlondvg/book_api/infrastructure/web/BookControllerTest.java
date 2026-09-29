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
import dev.marlondvg.book_api.domain.Rating;
import dev.marlondvg.book_api.domain.ReadingStatus;
import dev.marlondvg.book_api.domain.exception.BookNotFoundException;
import dev.marlondvg.book_api.domain.exception.InvalidStatusTransitionException;
import dev.marlondvg.book_api.domain.exception.RatingNotAllowedException;
import dev.marlondvg.book_api.infrastructure.config.CorsConfig;
import dev.marlondvg.book_api.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static dev.marlondvg.book_api.domain.ReadingStatus.READ;
import static dev.marlondvg.book_api.domain.ReadingStatus.READING;
import static dev.marlondvg.book_api.domain.ReadingStatus.TO_READ;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class BookControllerTest {

	private static final UUID OWNER_ID = UUID.randomUUID();
	private static final UUID BOOK_ID = UUID.randomUUID();
	private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
	private static final String DETAILS_JSON = """
			{"title": "Dune", "author": "Frank Herbert", "pages": 412, "isbn": "9780441013593"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CreateBookUseCase createBook;
	@MockitoBean
	private GetBookUseCase getBook;
	@MockitoBean
	private ListBooksUseCase listBooks;
	@MockitoBean
	private UpdateBookDetailsUseCase updateBookDetails;
	@MockitoBean
	private ChangeBookStatusUseCase changeBookStatus;
	@MockitoBean
	private RateBookUseCase rateBook;
	@MockitoBean
	private DeleteBookUseCase deleteBook;

	private static RequestPostProcessor owner() {
		return jwt().jwt(token -> token.subject(OWNER_ID.toString()));
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
		return request.with(owner()).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static Book book(ReadingStatus status, Rating rating) {
		LocalDate startedAt = status == TO_READ ? null : LocalDate.of(2026, 2, 1);
		LocalDate finishedAt = status == READ ? LocalDate.of(2026, 3, 1) : null;
		return Book.restore(BOOK_ID, OWNER_ID, "Dune", "Frank Herbert", 412, "9780441013593", null,
				status, rating, startedAt, finishedAt, CREATED_AT);
	}

	@Nested
	class Create {

		@Test
		void shouldCreateBookForCurrentUserAndReturnLocation() throws Exception {
			when(createBook.createBook(any())).thenReturn(book(TO_READ, null));

			mockMvc.perform(json(post("/api/books"), DETAILS_JSON))
					.andExpect(status().isCreated())
					.andExpect(header().string("Location", endsWith("/api/books/" + BOOK_ID)))
					.andExpect(jsonPath("$.id").value(BOOK_ID.toString()))
					.andExpect(jsonPath("$.title").value("Dune"))
					.andExpect(jsonPath("$.status").value("TO_READ"))
					.andExpect(jsonPath("$.rating").isEmpty())
					.andExpect(jsonPath("$.createdAt").value("2026-01-01T10:00:00Z"))
					.andExpect(jsonPath("$.ownerId").doesNotExist());

			verify(createBook).createBook(
					new CreateBookCommand(OWNER_ID, "Dune", "Frank Herbert", 412, "9780441013593", null));
		}

		@Test
		void shouldIgnoreOwnerIdSentInBody() throws Exception {
			when(createBook.createBook(any())).thenReturn(book(TO_READ, null));
			String body = """
					{"title": "Dune", "author": "Frank Herbert", "ownerId": "%s"}
					""".formatted(UUID.randomUUID());

			mockMvc.perform(json(post("/api/books"), body))
					.andExpect(status().isCreated());

			verify(createBook).createBook(
					new CreateBookCommand(OWNER_ID, "Dune", "Frank Herbert", null, null, null));
		}

		@Test
		void shouldRejectInvalidRequestWithFieldErrors() throws Exception {
			String body = """
					{"title": " ", "author": "Frank Herbert", "pages": 0}
					""";

			mockMvc.perform(json(post("/api/books"), body))
					.andExpect(status().isBadRequest())
					.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
					.andExpect(jsonPath("$.status").value(400))
					.andExpect(jsonPath("$.detail").value("Request validation failed"))
					.andExpect(jsonPath("$.errors", hasSize(2)))
					.andExpect(jsonPath("$.errors[?(@.field == 'title')]").exists())
					.andExpect(jsonPath("$.errors[?(@.field == 'pages')]").exists());

			verifyNoInteractions(createBook);
		}

		@Test
		void shouldRejectMalformedJson() throws Exception {
			mockMvc.perform(json(post("/api/books"), "{\"title\": "))
					.andExpect(status().isBadRequest())
					.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

			verifyNoInteractions(createBook);
		}
	}

	@Nested
	class Read {

		@Test
		void shouldListBooksFilteredByStatus() throws Exception {
			when(listBooks.listBooks(OWNER_ID, READING)).thenReturn(List.of(book(READING, null)));

			mockMvc.perform(get("/api/books").param("status", "READING").with(owner()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(1)))
					.andExpect(jsonPath("$[0].status").value("READING"))
					.andExpect(jsonPath("$[0].startedAt").value("2026-02-01"));
		}

		@Test
		void shouldListAllBooksWithoutStatusFilter() throws Exception {
			when(listBooks.listBooks(OWNER_ID, null)).thenReturn(List.of());

			mockMvc.perform(get("/api/books").with(owner()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(0)));
		}

		@Test
		void shouldRejectUnknownStatusFilter() throws Exception {
			mockMvc.perform(get("/api/books").param("status", "FINISHED").with(owner()))
					.andExpect(status().isBadRequest())
					.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

			verifyNoInteractions(listBooks);
		}

		@Test
		void shouldReturnBook() throws Exception {
			when(getBook.getBook(OWNER_ID, BOOK_ID)).thenReturn(book(READ, new Rating(5)));

			mockMvc.perform(get("/api/books/{id}", BOOK_ID).with(owner()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.rating").value(5))
					.andExpect(jsonPath("$.finishedAt").value("2026-03-01"));
		}

		@Test
		void shouldReturnNotFoundProblem() throws Exception {
			when(getBook.getBook(OWNER_ID, BOOK_ID)).thenThrow(new BookNotFoundException(BOOK_ID));

			mockMvc.perform(get("/api/books/{id}", BOOK_ID).with(owner()))
					.andExpect(status().isNotFound())
					.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
					.andExpect(jsonPath("$.status").value(404))
					.andExpect(jsonPath("$.title").value("Not Found"))
					.andExpect(jsonPath("$.instance").value("/api/books/" + BOOK_ID));
		}

		@Test
		void shouldRejectInvalidBookId() throws Exception {
			mockMvc.perform(get("/api/books/{id}", "not-a-uuid").with(owner()))
					.andExpect(status().isBadRequest());

			verifyNoInteractions(getBook);
		}
	}

	@Nested
	class Update {

		@Test
		void shouldUpdateDetails() throws Exception {
			when(updateBookDetails.updateDetails(any())).thenReturn(book(TO_READ, null));

			mockMvc.perform(json(put("/api/books/{id}", BOOK_ID), DETAILS_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(BOOK_ID.toString()));

			verify(updateBookDetails).updateDetails(new UpdateBookDetailsCommand(
					OWNER_ID, BOOK_ID, "Dune", "Frank Herbert", 412, "9780441013593", null));
		}

		@Test
		void shouldChangeStatus() throws Exception {
			when(changeBookStatus.changeStatus(any())).thenReturn(book(READING, null));

			mockMvc.perform(json(put("/api/books/{id}/status", BOOK_ID), "{\"status\": \"READING\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value("READING"));

			verify(changeBookStatus).changeStatus(new ChangeBookStatusCommand(OWNER_ID, BOOK_ID, READING));
		}

		@Test
		void shouldReturnConflictForInvalidTransition() throws Exception {
			when(changeBookStatus.changeStatus(any()))
					.thenThrow(new InvalidStatusTransitionException(READ, TO_READ));

			mockMvc.perform(json(put("/api/books/{id}/status", BOOK_ID), "{\"status\": \"TO_READ\"}"))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.detail").value("Cannot change status from READ to TO_READ"));
		}

		@Test
		void shouldRejectMissingOrUnknownStatus() throws Exception {
			mockMvc.perform(json(put("/api/books/{id}/status", BOOK_ID), "{}"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errors[0].field").value("status"));
			mockMvc.perform(json(put("/api/books/{id}/status", BOOK_ID), "{\"status\": \"FINISHED\"}"))
					.andExpect(status().isBadRequest());

			verifyNoInteractions(changeBookStatus);
		}

		@Test
		void shouldRateBook() throws Exception {
			when(rateBook.rate(any())).thenReturn(book(READ, new Rating(4)));

			mockMvc.perform(json(put("/api/books/{id}/rating", BOOK_ID), "{\"value\": 4}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.rating").value(4));

			verify(rateBook).rate(new RateBookCommand(OWNER_ID, BOOK_ID, 4));
		}

		@Test
		void shouldRejectRatingOutOfRange() throws Exception {
			mockMvc.perform(json(put("/api/books/{id}/rating", BOOK_ID), "{\"value\": 6}"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errors[0].field").value("value"));

			verifyNoInteractions(rateBook);
		}

		@Test
		void shouldReturnConflictWhenRatingIsNotAllowed() throws Exception {
			when(rateBook.rate(any())).thenThrow(new RatingNotAllowedException(READING));

			mockMvc.perform(json(put("/api/books/{id}/rating", BOOK_ID), "{\"value\": 4}"))
					.andExpect(status().isConflict());
		}

		@Test
		void shouldClearRating() throws Exception {
			when(rateBook.clearRating(OWNER_ID, BOOK_ID)).thenReturn(book(READ, null));

			mockMvc.perform(delete("/api/books/{id}/rating", BOOK_ID).with(owner()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.rating").isEmpty());
		}
	}

	@Nested
	class Delete {

		@Test
		void shouldDeleteBook() throws Exception {
			mockMvc.perform(delete("/api/books/{id}", BOOK_ID).with(owner()))
					.andExpect(status().isNoContent());

			verify(deleteBook).deleteBook(OWNER_ID, BOOK_ID);
		}
	}

	@Nested
	class Security {

		@Test
		void shouldRejectRequestWithoutAuthentication() throws Exception {
			mockMvc.perform(get("/api/books"))
					.andExpect(status().isUnauthorized())
					.andExpect(header().string("WWW-Authenticate", startsWith("Bearer")))
					.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
					.andExpect(jsonPath("$.status").value(401))
					.andExpect(jsonPath("$.detail").value("Authentication is required"))
					.andExpect(jsonPath("$.instance").value("/api/books"));

			verifyNoInteractions(listBooks);
		}

		@Test
		void shouldRejectTokenWhoseSubjectIsNotAUserId() throws Exception {
			mockMvc.perform(get("/api/books").with(jwt().jwt(token -> token.subject("admin"))))
					.andExpect(status().isUnauthorized())
					.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

			verifyNoInteractions(listBooks);
		}

		@Test
		void shouldHideDetailsOfUnexpectedErrors() throws Exception {
			when(getBook.getBook(OWNER_ID, BOOK_ID)).thenThrow(new IllegalStateException("db password is hunter2"));

			mockMvc.perform(get("/api/books/{id}", BOOK_ID).with(owner()))
					.andExpect(status().isInternalServerError())
					.andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
					.andExpect(content().string(not(containsString("hunter2"))))
					.andExpect(content().string(not(containsString("IllegalStateException"))));
		}
	}
}
