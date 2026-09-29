package dev.marlondvg.book_api.infrastructure.persistence;

import dev.marlondvg.book_api.domain.User;
import dev.marlondvg.book_api.domain.exception.EmailAlreadyUsedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserPersistenceAdapter.class)
class UserPersistenceAdapterTest {

	private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

	@Autowired
	private UserPersistenceAdapter adapter;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void shouldSaveAndFindUserByEmail() {
		User user = User.register("ann@example.com", "hashed", CREATED_AT);
		adapter.save(user);
		entityManager.clear();

		User found = adapter.findByEmail("ann@example.com").orElseThrow();

		assertThat(found.getId()).isEqualTo(user.getId());
		assertThat(found.getEmail()).isEqualTo("ann@example.com");
		assertThat(found.getPasswordHash()).isEqualTo("hashed");
		assertThat(found.getCreatedAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void shouldReportWhetherEmailExists() {
		adapter.save(User.register("ann@example.com", "hashed", CREATED_AT));

		assertThat(adapter.existsByEmail("ann@example.com")).isTrue();
		assertThat(adapter.existsByEmail("bob@example.com")).isFalse();
		assertThat(adapter.findByEmail("bob@example.com")).isEmpty();
	}

	@Test
	void shouldFindUserById() {
		User user = User.register("ann@example.com", "hashed", CREATED_AT);
		adapter.save(user);
		entityManager.clear();

		User found = adapter.findById(user.getId()).orElseThrow();

		assertThat(found.getEmail()).isEqualTo("ann@example.com");
		assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
	}

	@Test
	void shouldTranslateDuplicateEmailToDomainException() {
		adapter.save(User.register("ann@example.com", "hashed", CREATED_AT));

		assertThatThrownBy(() -> adapter.save(User.register("ann@example.com", "other", CREATED_AT)))
				.isInstanceOf(EmailAlreadyUsedException.class);
	}
}
