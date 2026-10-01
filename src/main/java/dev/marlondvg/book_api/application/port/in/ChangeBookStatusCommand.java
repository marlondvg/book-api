package dev.marlondvg.book_api.application.port.in;

import dev.marlondvg.book_api.domain.ReadingStatus;

import java.time.ZoneId;
import java.util.UUID;

/**
 * {@code timeZone} is where the user is, used to decide today's date. {@code null} means the server's clock zone.
 */
public record ChangeBookStatusCommand(UUID ownerId, UUID bookId, ReadingStatus status, ZoneId timeZone) {
}
