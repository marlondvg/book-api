package dev.marlondvg.book_api.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BookDetailsRequest(
		@NotBlank @Size(max = 255) String title,
		@NotBlank @Size(max = 255) String author,
		@Positive Integer pages,
		@Size(max = 20) String isbn,
		@Size(max = 500) String coverUrl) {
}
