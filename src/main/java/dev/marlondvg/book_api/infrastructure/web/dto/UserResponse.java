package dev.marlondvg.book_api.infrastructure.web.dto;

import dev.marlondvg.book_api.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String email, Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt());
	}
}
