package dev.marlondvg.book_api.infrastructure.web.dto;

import dev.marlondvg.book_api.application.port.out.AccessToken;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

	public static TokenResponse from(AccessToken token) {
		return new TokenResponse(token.value(), "Bearer", token.expiresIn().toSeconds());
	}

	@Override
	public String toString() {
		return "TokenResponse[accessToken=***, tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
	}
}
