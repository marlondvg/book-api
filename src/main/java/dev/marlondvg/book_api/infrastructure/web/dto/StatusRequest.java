package dev.marlondvg.book_api.infrastructure.web.dto;

import dev.marlondvg.book_api.domain.ReadingStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull ReadingStatus status) {
}
