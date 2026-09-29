package dev.marlondvg.book_api.application.service;

import dev.marlondvg.book_api.application.port.in.GetCurrentUserUseCase;
import dev.marlondvg.book_api.application.port.out.UserRepository;
import dev.marlondvg.book_api.domain.User;
import dev.marlondvg.book_api.domain.exception.UnknownUserException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService implements GetCurrentUserUseCase {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public User getCurrentUser(UUID userId) {
		return userRepository.findById(userId).orElseThrow(UnknownUserException::new);
	}
}
