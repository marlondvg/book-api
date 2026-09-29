package dev.marlondvg.book_api.infrastructure.security;

import dev.marlondvg.book_api.application.port.out.AccessToken;
import dev.marlondvg.book_api.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenIssuerTest {

	private static final String SECRET = "a-test-secret-that-is-long-enough-for-hs256";
	private static final JwtProperties PROPERTIES = new JwtProperties(SECRET, Duration.ofHours(1), "book-api");

	private final JwtDecoder decoder = new SecurityConfig().jwtDecoder(PROPERTIES);
	private final User user = User.restore(java.util.UUID.randomUUID(), "ann@example.com", "hash", Instant.EPOCH);

	private static JwtTokenIssuer issuerAt(Instant now, JwtProperties properties) {
		return new JwtTokenIssuer(properties, Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	void shouldIssueTokenThatDecodesToTheUser() {
		Instant now = Instant.now();

		AccessToken token = issuerAt(now, PROPERTIES).issueFor(user);
		Jwt jwt = decoder.decode(token.value());

		assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
		assertThat(jwt.getClaimAsString("iss")).isEqualTo("book-api");
		assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(Duration.ofHours(1)).truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
		assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
		assertThat(token.expiresIn()).isEqualTo(Duration.ofHours(1));
		assertThat(token.toString()).doesNotContain(token.value());
	}

	@Test
	void shouldRejectTokenSignedWithAnotherKey() {
		JwtProperties otherKey = new JwtProperties("another-secret-that-is-also-long-enough-x", Duration.ofHours(1),
				"book-api");
		String token = issuerAt(Instant.now(), otherKey).issueFor(user).value();

		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void shouldRejectExpiredToken() {
		String token = issuerAt(Instant.now().minus(Duration.ofHours(2)), PROPERTIES).issueFor(user).value();

		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void shouldRejectTokenFromAnotherIssuer() {
		JwtProperties otherIssuer = new JwtProperties(SECRET, Duration.ofHours(1), "someone-else");
		String token = issuerAt(Instant.now(), otherIssuer).issueFor(user).value();

		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void shouldRefuseMissingOrShortSecret() {
		assertThatThrownBy(() -> new JwtProperties(null, Duration.ofHours(1), "book-api"))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new JwtProperties("too-short", Duration.ofHours(1), "book-api"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldNotShowSecretInPropertiesToString() {
		assertThat(PROPERTIES.toString()).doesNotContain(SECRET);
	}
}
