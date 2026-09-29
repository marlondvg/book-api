package dev.marlondvg.book_api.application.port.in;

import dev.marlondvg.book_api.domain.User;

import java.util.UUID;

public interface GetCurrentUserUseCase {

	User getCurrentUser(UUID userId);
}
