package dev.marlondvg.book_api.application.port.out;

import dev.marlondvg.book_api.domain.User;

public interface TokenIssuer {

	AccessToken issueFor(User user);
}
