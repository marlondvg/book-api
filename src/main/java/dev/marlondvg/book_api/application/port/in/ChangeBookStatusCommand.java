package dev.marlondvg.book_api.application.port.in;

import dev.marlondvg.book_api.domain.ReadingStatus;

import java.util.UUID;

public record ChangeBookStatusCommand(UUID ownerId, UUID bookId, ReadingStatus status) {
}
