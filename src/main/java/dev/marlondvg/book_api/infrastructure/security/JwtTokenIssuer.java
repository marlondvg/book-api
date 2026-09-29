package dev.marlondvg.book_api.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import dev.marlondvg.book_api.application.port.out.AccessToken;
import dev.marlondvg.book_api.application.port.out.TokenIssuer;
import dev.marlondvg.book_api.domain.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
class JwtTokenIssuer implements TokenIssuer {

	private final JwtEncoder encoder;
	private final JwtProperties properties;
	private final Clock clock;

	JwtTokenIssuer(JwtProperties properties, Clock clock) {
		this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(properties.secretKey()));
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	public AccessToken issueFor(User user) {
		Instant now = Instant.now(clock);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.subject(user.getId().toString())
				.issuedAt(now)
				.expiresAt(now.plus(properties.ttl()))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new AccessToken(token, properties.ttl());
	}
}
