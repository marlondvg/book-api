package dev.marlondvg.book_api.infrastructure.web;

import dev.marlondvg.book_api.application.port.in.GetCurrentUserUseCase;
import dev.marlondvg.book_api.domain.User;
import dev.marlondvg.book_api.domain.exception.UnknownUserException;
import dev.marlondvg.book_api.infrastructure.config.CorsConfig;
import dev.marlondvg.book_api.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class UserControllerTest {

	private static final UUID USER_ID = UUID.randomUUID();

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GetCurrentUserUseCase getCurrentUser;

	@Test
	void shouldReturnCurrentUserFromTokenSubject() throws Exception {
		User user = User.restore(USER_ID, "ann@example.com", "stored-hash", Instant.parse("2026-01-01T10:00:00Z"));
		when(getCurrentUser.getCurrentUser(USER_ID)).thenReturn(user);

		mockMvc.perform(get("/api/users/me").with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(USER_ID.toString()))
				.andExpect(jsonPath("$.email").value("ann@example.com"))
				.andExpect(jsonPath("$.createdAt").value("2026-01-01T10:00:00Z"))
				.andExpect(content().string(not(containsString("stored-hash"))));
	}

	@Test
	void shouldReturnUnauthorizedWhenUserNoLongerExists() throws Exception {
		when(getCurrentUser.getCurrentUser(USER_ID)).thenThrow(new UnknownUserException());

		mockMvc.perform(get("/api/users/me").with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("User no longer exists"));
	}

	@Test
	void shouldRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

		verifyNoInteractions(getCurrentUser);
	}
}
