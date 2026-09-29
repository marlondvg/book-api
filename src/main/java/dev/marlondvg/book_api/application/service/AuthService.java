package dev.marlondvg.book_api.application.service;

import dev.marlondvg.book_api.application.port.in.LoginCommand;
import dev.marlondvg.book_api.application.port.in.LoginUseCase;
import dev.marlondvg.book_api.application.port.in.RegisterUserCommand;
import dev.marlondvg.book_api.application.port.in.RegisterUserUseCase;
import dev.marlondvg.book_api.application.port.out.AccessToken;
import dev.marlondvg.book_api.application.port.out.LoginAttemptLimiter;
import dev.marlondvg.book_api.application.port.out.PasswordHasher;
import dev.marlondvg.book_api.application.port.out.TokenIssuer;
import dev.marlondvg.book_api.application.port.out.UserRepository;
import dev.marlondvg.book_api.domain.PasswordPolicy;
import dev.marlondvg.book_api.domain.User;
import dev.marlondvg.book_api.domain.exception.EmailAlreadyUsedException;
import dev.marlondvg.book_api.domain.exception.InvalidCredentialsException;
import dev.marlondvg.book_api.domain.exception.InvalidUserException;
import dev.marlondvg.book_api.domain.exception.TooManyLoginAttemptsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class AuthService implements RegisterUserUseCase, LoginUseCase {

	private final UserRepository userRepository;
	private final PasswordHasher passwordHasher;
	private final TokenIssuer tokenIssuer;
	private final LoginAttemptLimiter loginAttemptLimiter;
	private final Clock clock;

	public AuthService(UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer,
			LoginAttemptLimiter loginAttemptLimiter, Clock clock) {
		this.userRepository = userRepository;
		this.passwordHasher = passwordHasher;
		this.tokenIssuer = tokenIssuer;
		this.loginAttemptLimiter = loginAttemptLimiter;
		this.clock = clock;
	}

	@Override
	public User register(RegisterUserCommand command) {
		String email = User.normalizeEmail(command.email());
		PasswordPolicy.validate(command.password());
		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyUsedException();
		}
		User user = User.register(email, passwordHasher.hash(command.password()), Instant.now(clock));
		return userRepository.save(user);
	}

	@Override
	@Transactional(readOnly = true)
	public AccessToken login(LoginCommand command) {
		String email = normalizeOrNull(command.email());
		// Checked before any hashing, so blocked attempts cost no BCrypt time.
		loginAttemptLimiter.retryAfter(email, command.clientIp()).ifPresent(retryAfter -> {
			throw new TooManyLoginAttemptsException(retryAfter);
		});
		try {
			User user = authenticate(email, command.password());
			loginAttemptLimiter.recordSuccess(email);
			return tokenIssuer.issueFor(user);
		} catch (InvalidCredentialsException e) {
			loginAttemptLimiter.recordFailure(email, command.clientIp());
			throw e;
		}
	}

	private User authenticate(String email, String password) {
		if (password == null || PasswordPolicy.exceedsMaxBytes(password)) {
			throw new InvalidCredentialsException();
		}
		Optional<User> user = email == null ? Optional.empty() : userRepository.findByEmail(email);
		if (user.isEmpty()) {
			// Spend the same time as a real check so response time does not reveal unknown emails.
			passwordHasher.hash(password);
			throw new InvalidCredentialsException();
		}
		if (!passwordHasher.matches(password, user.get().getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
		return user.get();
	}

	/**
	 * Returns the normalized email, or {@code null} if it is malformed. A malformed
	 * email cannot belong to an account, so login answers like any unknown email.
	 */
	private static String normalizeOrNull(String rawEmail) {
		try {
			return User.normalizeEmail(rawEmail);
		} catch (InvalidUserException invalidEmail) {
			return null;
		}
	}
}
