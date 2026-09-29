package dev.marlondvg.book_api.infrastructure.web;

import dev.marlondvg.book_api.application.port.in.LoginCommand;
import dev.marlondvg.book_api.application.port.in.LoginUseCase;
import dev.marlondvg.book_api.application.port.in.RegisterUserCommand;
import dev.marlondvg.book_api.application.port.in.RegisterUserUseCase;
import dev.marlondvg.book_api.infrastructure.web.dto.LoginRequest;
import dev.marlondvg.book_api.infrastructure.web.dto.RegisterRequest;
import dev.marlondvg.book_api.infrastructure.web.dto.TokenResponse;
import dev.marlondvg.book_api.infrastructure.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@SecurityRequirements // public endpoints: no bearer token in Swagger
class AuthController {

	private final RegisterUserUseCase registerUser;
	private final LoginUseCase login;

	AuthController(RegisterUserUseCase registerUser, LoginUseCase login) {
		this.registerUser = registerUser;
		this.login = login;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	UserResponse register(@Valid @RequestBody RegisterRequest request) {
		return UserResponse.from(registerUser.register(new RegisterUserCommand(request.email(), request.password())));
	}

	@PostMapping("/login")
	TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		// In production the remote address comes from X-Forwarded-For (forward-headers-strategy).
		return TokenResponse.from(login.login(
				new LoginCommand(request.email(), request.password(), httpRequest.getRemoteAddr())));
	}
}
