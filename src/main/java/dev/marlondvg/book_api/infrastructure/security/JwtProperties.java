package dev.marlondvg.book_api.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * JWT settings. The secret comes from the {@code JWT_SECRET} environment variable.
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(
		String secret,
		@DefaultValue("1h") Duration ttl,
		@DefaultValue("book-api") String issuer) {

	// HS256 needs a key of at least 256 bits.
	private static final int MIN_SECRET_BYTES = 32;

	public JwtProperties {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"app.jwt.secret (JWT_SECRET) must be set and at least " + MIN_SECRET_BYTES + " bytes long");
		}
	}

	SecretKey secretKey() {
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Override
	public String toString() {
		return "JwtProperties[secret=***, ttl=" + ttl + ", issuer=" + issuer + "]";
	}
}
