package dev.marlondvg.book_api.infrastructure.persistence;

import dev.marlondvg.book_api.application.port.out.UserRepository;
import dev.marlondvg.book_api.domain.User;
import dev.marlondvg.book_api.domain.exception.EmailAlreadyUsedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class UserPersistenceAdapter implements UserRepository {

	private final SpringDataUserRepository repository;

	UserPersistenceAdapter(SpringDataUserRepository repository) {
		this.repository = repository;
	}

	@Override
	public User save(User user) {
		UserJpaEntity entity = new UserJpaEntity(user.getId(), user.getEmail(), user.getPasswordHash(),
				user.getCreatedAt());
		try {
			// Flush now so a duplicate email (unique constraint) fails here, not at commit.
			return toDomain(repository.saveAndFlush(entity));
		} catch (DataIntegrityViolationException e) {
			// The only constraint the domain cannot check itself is the unique email.
			throw new EmailAlreadyUsedException();
		}
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return repository.findByEmail(email).map(UserPersistenceAdapter::toDomain);
	}

	@Override
	public boolean existsByEmail(String email) {
		return repository.existsByEmail(email);
	}

	private static User toDomain(UserJpaEntity entity) {
		return User.restore(entity.getId(), entity.getEmail(), entity.getPasswordHash(), entity.getCreatedAt());
	}
}
