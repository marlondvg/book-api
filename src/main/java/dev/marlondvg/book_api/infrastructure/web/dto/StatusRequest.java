package dev.marlondvg.book_api.infrastructure.web.dto;

import dev.marlondvg.book_api.domain.ReadingStatus;
import jakarta.validation.constraints.NotNull;

import java.time.ZoneId;

public record StatusRequest(@NotNull ReadingStatus status, ZoneId timeZone) {
}
