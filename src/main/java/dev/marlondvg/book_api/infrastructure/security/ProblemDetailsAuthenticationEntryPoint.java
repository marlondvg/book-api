package dev.marlondvg.book_api.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Answers unauthenticated requests with 401, a {@code WWW-Authenticate: Bearer}
 * header and the same problem-details body as the rest of the API.
 */
final class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final BearerTokenAuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
	private final ObjectMapper objectMapper;

	ProblemDetailsAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		bearerEntryPoint.commence(request, response, authException);

		String detail = authException instanceof OAuth2AuthenticationException
				? "The access token is invalid or expired"
				: "Authentication is required";
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", "about:blank");
		body.put("title", HttpStatus.UNAUTHORIZED.getReasonPhrase());
		body.put("status", HttpStatus.UNAUTHORIZED.value());
		body.put("detail", detail);
		body.put("instance", request.getRequestURI());

		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
