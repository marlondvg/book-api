package dev.marlondvg.book_api.application.service;

import dev.marlondvg.book_api.application.port.in.LoginCommand;
import dev.marlondvg.book_api.application.port.in.LoginUseCase;
import dev.marlondvg.book_api.application.port.in.RegisterUserCommand;
import dev.marlondvg.book_api.application.port.in.RegisterUserUseCase;
import dev.marlondvg.book_api.application.port.out.AccessToken;
import dev.marlondvg.book_api.application.port.out.PasswordHasher;
import dev.marlondvg.book_api.application.port.out.TokenIssuer;
import dev.marlondvg.book_api.application.port.out.UserRepository;
import dev.marlondvg.book_api.domain.PasswordPolicy;
import dev.marlondvg.book_api.domain.User;
import dev.marlondvg.book_api.domain.exception.EmailAlreadyUsedException;
import dev.marlondvg.book_api.domain.exception.InvalidCredentialsException;
import dev.marlondvg.book_api.domain.exception.InvalidUserException;
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
	private final Clock clock;

	public AuthService(UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer,
			Clock clock) {
		this.userRepository = userRepository;
		this.passwordHasher = passwordHasher;
		this.tokenIssuer = tokenIssuer;
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
		String password = command.password();
		if (password == null || PasswordPolicy.exceedsMaxBytes(password)) {
			throw new InvalidCredentialsException();
		}
		Optional<User> user = findByEmail(command.email());
		if (user.isEmpty()) {
			// Spend the same time as a real check so response time does not reveal unknown emails.
			passwordHasher.hash(password);
			throw new InvalidCredentialsException();
		}
		if (!passwordHasher.matches(password, user.get().getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
		return tokenIssuer.issueFor(user.get());
	}

	private Optional<User> findByEmail(String rawEmail) {
		String email;
		try {
			email = User.normalizeEmail(rawEmail);
		} catch (InvalidUserException invalidEmail) {
			// An invalid email cannot belong to an account; answer like any unknown email.
			return Optional.empty();
		}
		return userRepository.findByEmail(email);
	}
}
