package dev.marlondvg.book_api.infrastructure.web;

import dev.marlondvg.book_api.application.port.in.GetCurrentUserUseCase;
import dev.marlondvg.book_api.infrastructure.security.CurrentUserId;
import dev.marlondvg.book_api.infrastructure.web.dto.UserResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
class UserController {

	private final GetCurrentUserUseCase getCurrentUser;

	UserController(GetCurrentUserUseCase getCurrentUser) {
		this.getCurrentUser = getCurrentUser;
	}

	@GetMapping("/me")
	UserResponse me(@CurrentUserId UUID userId) {
		return UserResponse.from(getCurrentUser.getCurrentUser(userId));
	}
}
