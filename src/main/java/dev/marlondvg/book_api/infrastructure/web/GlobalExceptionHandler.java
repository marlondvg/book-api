package dev.marlondvg.book_api.infrastructure.web;

import dev.marlondvg.book_api.domain.exception.BookNotFoundException;
import dev.marlondvg.book_api.domain.exception.DomainException;
import dev.marlondvg.book_api.domain.exception.InvalidBookException;
import dev.marlondvg.book_api.domain.exception.InvalidRatingException;
import dev.marlondvg.book_api.domain.exception.InvalidStatusTransitionException;
import dev.marlondvg.book_api.domain.exception.RatingNotAllowedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Turns every error into an RFC 9457 problem detail. Spring MVC's own errors
 * (malformed JSON, type mismatches, ...) are handled by the base class.
 */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BookNotFoundException.class)
	ProblemDetail handleNotFound(BookNotFoundException ex, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler({InvalidBookException.class, InvalidRatingException.class})
	ProblemDetail handleInvalidInput(DomainException ex, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler({InvalidStatusTransitionException.class, RatingNotAllowedException.class})
	ProblemDetail handleConflict(DomainException ex, HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail handleUnauthenticated(AuthenticationException ex, HttpServletRequest request) {
		return problem(HttpStatus.UNAUTHORIZED, "Authentication is required", request);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> Map.of(
						"field", error.getField(),
						"message", String.valueOf(error.getDefaultMessage())))
				.toList();
		ProblemDetail body = ex.getBody();
		body.setDetail("Request validation failed");
		body.setProperty("errors", errors);
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	private static ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setInstance(URI.create(request.getRequestURI()));
		return problem;
	}
}
