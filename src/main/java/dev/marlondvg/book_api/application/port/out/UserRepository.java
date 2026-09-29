package dev.marlondvg.book_api.application.port.out;

import dev.marlondvg.book_api.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

	/**
	 * Saves a new user.
	 *
	 * @throws dev.marlondvg.book_api.domain.exception.EmailAlreadyUsedException if the email is taken
	 */
	User save(User user);

	/**
	 * Finds a user by an email that is already normalized.
	 */
	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	Optional<User> findById(UUID id);
}
