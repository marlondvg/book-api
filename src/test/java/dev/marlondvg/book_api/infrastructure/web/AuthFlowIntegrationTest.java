package dev.marlondvg.book_api.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real tokens end to end: register, log in, then call the book API with the issued JWT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {

	private static final String PASSWORD = "correct horse battery";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
		return request.contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private void register(String email) throws Exception {
		mockMvc.perform(json(post("/api/auth/register"), """
						{"email": "%s", "password": "%s"}
						""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated());
	}

	private String login(String email) throws Exception {
		String body = mockMvc.perform(json(post("/api/auth/login"), """
						{"email": "%s", "password": "%s"}
						""".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	@Test
	void shouldRegisterLoginAndUseTokenForBooks() throws Exception {
		register("Ann@Example.com");
		String token = login("ann@example.com");

		mockMvc.perform(json(post("/api/books"), "{\"title\": \"Dune\", \"author\": \"Frank Herbert\"}")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/api/books").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].title").value("Dune"));
	}

	@Test
	void shouldStoreBcryptHashAndNormalizedEmail() throws Exception {
		register("Bob@Example.COM");

		String hash = jdbcTemplate.queryForObject(
				"SELECT password_hash FROM users WHERE email = ?", String.class, "bob@example.com");
		assertThat(hash).startsWith("$2").isNotEqualTo(PASSWORD);
	}

	@Test
	void shouldRejectSecondRegistrationWithSameEmailInAnyCase() throws Exception {
		register("carol@example.com");

		mockMvc.perform(json(post("/api/auth/register"), """
						{"email": "CAROL@example.com", "password": "%s"}
						""".formatted(PASSWORD)))
				.andExpect(status().isConflict());
	}

	@Test
	void shouldGiveSameAnswerForWrongPasswordAndUnknownEmail() throws Exception {
		register("dan@example.com");

		String wrongPassword = mockMvc.perform(json(post("/api/auth/login"), """
						{"email": "dan@example.com", "password": "not the password"}
						"""))
				.andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();
		String unknownEmail = mockMvc.perform(json(post("/api/auth/login"), """
						{"email": "nobody@example.com", "password": "not the password"}
						"""))
				.andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();

		assertThat(JsonPath.<String>read(wrongPassword, "$.detail"))
				.isEqualTo(JsonPath.<String>read(unknownEmail, "$.detail"))
				.isEqualTo("Invalid email or password");
	}

	@Test
	void shouldRejectRequestWithoutToken() throws Exception {
		mockMvc.perform(get("/api/books"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Authentication is required"));
	}

	@Test
	void shouldRejectInvalidToken() throws Exception {
		mockMvc.perform(get("/api/books").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.valid.token"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer error=\"invalid_token\"")))
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("The access token is invalid or expired"));
	}

	@Test
	void shouldExposeHealthAndApiDocsWithoutToken() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
	}

	@Test
	void shouldAllowCorsPreflightFromFrontendOrigin() throws Exception {
		mockMvc.perform(options("/api/books")
						.header(HttpHeaders.ORIGIN, "http://localhost:5173")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
	}

	@Test
	void shouldRefuseCorsPreflightFromUnknownOrigin() throws Exception {
		mockMvc.perform(options("/api/books")
						.header(HttpHeaders.ORIGIN, "https://evil.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}
}
