package dev.marlondvg.book_api.application.port.in;

import dev.marlondvg.book_api.domain.User;

public interface RegisterUserUseCase {

	User register(RegisterUserCommand command);
}
