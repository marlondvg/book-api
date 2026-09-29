package dev.marlondvg.book_api.application.port.in;

import dev.marlondvg.book_api.application.port.out.AccessToken;

public interface LoginUseCase {

	AccessToken login(LoginCommand command);
}
