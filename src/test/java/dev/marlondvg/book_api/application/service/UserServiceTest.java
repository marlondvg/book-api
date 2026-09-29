package dev.marlondvg.book_api.application.service;

import dev.marlondvg.book_api.application.port.out.UserRepository;
import dev.marlondvg.book_api.domain.User;
import dev.marlondvg.book_api.domain.exception.UnknownUserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserService userService;

	@Test
	void shouldReturnCurrentUser() {
		User user = User.restore(UUID.randomUUID(), "ann@example.com", "hash", Instant.EPOCH);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

		assertThat(userService.getCurrentUser(user.getId())).isSameAs(user);
	}

	@Test
	void shouldRejectUserThatNoLongerExists() {
		UUID userId = UUID.randomUUID();
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getCurrentUser(userId))
				.isInstanceOf(UnknownUserException.class)
				.hasMessage("User no longer exists");
	}
}
