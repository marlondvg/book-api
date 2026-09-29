package dev.marlondvg.book_api.infrastructure.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the whole stack (controller, service, JPA, H2 with Flyway) to check that
 * one user can never reach another user's book.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookOwnershipIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private UUID userA;
	private UUID userB;
	private String bookOfB;

	@BeforeEach
	void setUp() throws Exception {
		userA = insertUser("a@example.com");
		userB = insertUser("b@example.com");

		String location = mockMvc.perform(json(post("/api/books"), userB, """
						{"title": "Dune", "author": "Frank Herbert"}
						"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
		bookOfB = location.substring(location.lastIndexOf('/') + 1);
		mockMvc.perform(json(put("/api/books/{id}/status", bookOfB), userB, "{\"status\": \"READ\"}"))
				.andExpect(status().isOk());
	}

	private UUID insertUser(String email) {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update("INSERT INTO users (id, email, password_hash, created_at) VALUES (?, ?, ?, ?)",
				id, email, "not-a-real-hash", Timestamp.from(Instant.now()));
		return id;
	}

	private static RequestPostProcessor as(UUID user) {
		return jwt().jwt(token -> token.subject(user.toString()));
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, UUID user, String body) {
		return request.with(as(user)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	@Test
	void shouldNotLetUserReadAnotherUsersBook() throws Exception {
		mockMvc.perform(get("/api/books/{id}", bookOfB).with(as(userA)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/books").with(as(userA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void shouldNotLetUserModifyAnotherUsersBook() throws Exception {
		mockMvc.perform(json(put("/api/books/{id}", bookOfB), userA, """
						{"title": "Hacked", "author": "Someone"}
						"""))
				.andExpect(status().isNotFound());
		mockMvc.perform(json(put("/api/books/{id}/status", bookOfB), userA, "{\"status\": \"READING\"}"))
				.andExpect(status().isNotFound());
		mockMvc.perform(json(put("/api/books/{id}/rating", bookOfB), userA, "{\"value\": 1}"))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/books/{id}/rating", bookOfB).with(as(userA)).with(csrf()))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/books/{id}", bookOfB).with(as(userB)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Dune"))
				.andExpect(jsonPath("$.status").value("READ"))
				.andExpect(jsonPath("$.rating").isEmpty());
	}

	@Test
	void shouldNotLetUserDeleteAnotherUsersBook() throws Exception {
		mockMvc.perform(delete("/api/books/{id}", bookOfB).with(as(userA)).with(csrf()))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/books/{id}", bookOfB).with(as(userB)))
				.andExpect(status().isOk());
	}
}
