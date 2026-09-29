package dev.marlondvg.book_api.infrastructure.web;

import dev.marlondvg.book_api.application.port.in.LoginCommand;
import dev.marlondvg.book_api.application.port.in.LoginUseCase;
import dev.marlondvg.book_api.application.port.in.RegisterUserCommand;
import dev.marlondvg.book_api.application.port.in.RegisterUserUseCase;
import dev.marlondvg.book_api.application.port.out.AccessToken;
import dev.marlondvg.book_api.domain.User;
import dev.marlondvg.book_api.domain.exception.EmailAlreadyUsedException;
import dev.marlondvg.book_api.domain.exception.InvalidCredentialsException;
import dev.marlondvg.book_api.domain.exception.InvalidPasswordException;
import dev.marlondvg.book_api.domain.exception.TooManyLoginAttemptsException;
import dev.marlondvg.book_api.infrastructure.config.CorsConfig;
import dev.marlondvg.book_api.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class AuthControllerTest {

	private static final String PASSWORD = "correct horse battery";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RegisterUserUseCase registerUser;
	@MockitoBean
	private LoginUseCase login;

	private ResultActions postJson(String path, String body) throws Exception {
		return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void shouldRegisterWithoutAuthenticationAndNeverReturnPasswordHash() throws Exception {
		User user = User.restore(UUID.randomUUID(), "ann@example.com", "stored-hash",
				Instant.parse("2026-01-01T10:00:00Z"));
		when(registerUser.register(any())).thenReturn(user);

		postJson("/api/auth/register", """
				{"email": "ann@example.com", "password": "%s"}
				""".formatted(PASSWORD))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(user.getId().toString()))
				.andExpect(jsonPath("$.email").value("ann@example.com"))
				.andExpect(jsonPath("$.createdAt").value("2026-01-01T10:00:00Z"))
				.andExpect(content().string(not(containsString("stored-hash"))))
				.andExpect(content().string(not(containsString(PASSWORD))));

		verify(registerUser).register(new RegisterUserCommand("ann@example.com", PASSWORD));
	}

	@Test
	void shouldRejectInvalidRegistration() throws Exception {
		postJson("/api/auth/register", """
				{"email": "not-an-email", "password": "short"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
				.andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists());

		verifyNoInteractions(registerUser);
	}

	@Test
	void shouldReturnConflictForUsedEmail() throws Exception {
		when(registerUser.register(any())).thenThrow(new EmailAlreadyUsedException());

		postJson("/api/auth/register", """
				{"email": "ann@example.com", "password": "%s"}
				""".formatted(PASSWORD))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("An account with this email already exists"));
	}

	@Test
	void shouldReturnBadRequestWhenDomainRejectsPassword() throws Exception {
		when(registerUser.register(any())).thenThrow(new InvalidPasswordException("password must be at most 72 bytes"));

		postJson("/api/auth/register", """
				{"email": "ann@example.com", "password": "%s"}
				""".formatted("€".repeat(30)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("password must be at most 72 bytes"));
	}

	@Test
	void shouldLoginWithoutAuthenticationAndReturnBearerToken() throws Exception {
		when(login.login(any())).thenReturn(new AccessToken("signed.jwt.token", Duration.ofHours(1)));

		mockMvc.perform(post("/api/auth/login")
						.with(request -> {
							request.setRemoteAddr("10.1.2.3");
							return request;
						})
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "ann@example.com", "password": "%s"}
								""".formatted(PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("signed.jwt.token"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(3600));

		verify(login).login(new LoginCommand("ann@example.com", PASSWORD, "10.1.2.3"));
	}

	@Test
	void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {
		when(login.login(any())).thenThrow(new InvalidCredentialsException());

		postJson("/api/auth/login", """
				{"email": "ann@example.com", "password": "wrong password"}
				""")
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Invalid email or password"));
	}

	@Test
	void shouldReturnTooManyRequestsWithRetryAfterWhenLoginIsBlocked() throws Exception {
		when(login.login(any())).thenThrow(new TooManyLoginAttemptsException(Duration.ofMillis(90_500)));

		postJson("/api/auth/login", """
				{"email": "ann@example.com", "password": "%s"}
				""".formatted(PASSWORD))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("Retry-After", "91"))
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(429))
				.andExpect(jsonPath("$.detail").value("Too many failed login attempts. Try again later."));
	}

	@Test
	void shouldRejectLoginWithMissingFields() throws Exception {
		postJson("/api/auth/login", "{}")
				.andExpect(status().isBadRequest());

		verifyNoInteractions(login);
	}
}
