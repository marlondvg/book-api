package dev.marlondvg.book_api.application.port.in;

import java.util.UUID;

public record RateBookCommand(UUID ownerId, UUID bookId, int rating) {
}
