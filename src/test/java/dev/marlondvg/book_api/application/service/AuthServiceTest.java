package dev.marlondvg.book_api.application.service;

import dev.marlondvg.book_api.application.port.in.LoginCommand;
import dev.marlondvg.book_api.application.port.in.RegisterUserCommand;
import dev.marlondvg.book_api.application.port.out.AccessToken;
import dev.marlondvg.book_api.application.port.out.PasswordHasher;
import dev.marlondvg.book_api.application.port.out.TokenIssuer;
import dev.marlondvg.book_api.application.port.out.UserRepository;
import dev.marlondvg.book_api.domain.User;
import dev.marlondvg.book_api.domain.exception.EmailAlreadyUsedException;
import dev.marlondvg.book_api.domain.exception.InvalidCredentialsException;
import dev.marlondvg.book_api.domain.exception.InvalidPasswordException;
import dev.marlondvg.book_api.domain.exception.InvalidUserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final Instant NOW = Instant.parse("2026-04-01T12:00:00Z");
	private static final String PASSWORD = "correct horse battery";

	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordHasher passwordHasher;
	@Mock
	private TokenIssuer tokenIssuer;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(userRepository, passwordHasher, tokenIssuer, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Nested
	class Register {

		@Test
		void shouldSaveUserWithNormalizedEmailAndHashedPassword() {
			when(userRepository.existsByEmail("ann@example.com")).thenReturn(false);
			when(passwordHasher.hash(PASSWORD)).thenReturn("hashed");
			when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

			User user = authService.register(new RegisterUserCommand(" Ann@Example.com ", PASSWORD));

			assertThat(user.getEmail()).isEqualTo("ann@example.com");
			assertThat(user.getPasswordHash()).isEqualTo("hashed");
			assertThat(user.getCreatedAt()).isEqualTo(NOW);
			verify(userRepository).save(user);
		}

		@Test
		void shouldRejectEmailThatIsAlreadyUsed() {
			when(userRepository.existsByEmail("ann@example.com")).thenReturn(true);

			assertThatThrownBy(() -> authService.register(new RegisterUserCommand("ANN@example.com", PASSWORD)))
					.isInstanceOf(EmailAlreadyUsedException.class);

			verify(userRepository, never()).save(any());
			verifyNoInteractions(passwordHasher);
		}

		@Test
		void shouldRejectInvalidEmailBeforeHashing() {
			assertThatThrownBy(() -> authService.register(new RegisterUserCommand("not-an-email", PASSWORD)))
					.isInstanceOf(InvalidUserException.class);

			verifyNoInteractions(userRepository, passwordHasher);
		}

		@Test
		void shouldRejectWeakPasswordBeforeHashing() {
			assertThatThrownBy(() -> authService.register(new RegisterUserCommand("ann@example.com", "short")))
					.isInstanceOf(InvalidPasswordException.class);

			verifyNoInteractions(userRepository, passwordHasher);
		}

		@Test
		void shouldNotShowPasswordInCommandToString() {
			assertThat(new RegisterUserCommand("ann@example.com", PASSWORD).toString()).doesNotContain(PASSWORD);
			assertThat(new LoginCommand("ann@example.com", PASSWORD).toString()).doesNotContain(PASSWORD);
		}
	}

	@Nested
	class Login {

		private final User user = User.restore(java.util.UUID.randomUUID(), "ann@example.com", "stored-hash",
				NOW.minusSeconds(3600));

		@Test
		void shouldIssueTokenForValidCredentials() {
			AccessToken token = new AccessToken("jwt", Duration.ofHours(1));
			when(userRepository.findByEmail("ann@example.com")).thenReturn(Optional.of(user));
			when(passwordHasher.matches(PASSWORD, "stored-hash")).thenReturn(true);
			when(tokenIssuer.issueFor(user)).thenReturn(token);

			assertThat(authService.login(new LoginCommand(" ANN@example.com", PASSWORD))).isEqualTo(token);
		}

		@Test
		void shouldRejectWrongPassword() {
			when(userRepository.findByEmail("ann@example.com")).thenReturn(Optional.of(user));
			when(passwordHasher.matches("wrong password", "stored-hash")).thenReturn(false);

			assertThatThrownBy(() -> authService.login(new LoginCommand("ann@example.com", "wrong password")))
					.isInstanceOf(InvalidCredentialsException.class)
					.hasMessage("Invalid email or password");

			verifyNoInteractions(tokenIssuer);
		}

		@Test
		void shouldRejectUnknownEmailWithSameErrorAndStillHash() {
			when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> authService.login(new LoginCommand("nobody@example.com", PASSWORD)))
					.isInstanceOf(InvalidCredentialsException.class)
					.hasMessage("Invalid email or password");

			verify(passwordHasher).hash(PASSWORD);
			verifyNoInteractions(tokenIssuer);
		}

		@Test
		void shouldRejectMalformedEmailLikeUnknownEmail() {
			assertThatThrownBy(() -> authService.login(new LoginCommand("not-an-email", PASSWORD)))
					.isInstanceOf(InvalidCredentialsException.class);

			verify(passwordHasher).hash(PASSWORD);
			verifyNoInteractions(userRepository, tokenIssuer);
		}

		@Test
		void shouldRejectPasswordLongerThan72BytesWithoutHashing() {
			assertThatThrownBy(() -> authService.login(new LoginCommand("ann@example.com", "a".repeat(73))))
					.isInstanceOf(InvalidCredentialsException.class);

			verifyNoInteractions(userRepository, passwordHasher, tokenIssuer);
		}
	}
}
