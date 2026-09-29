package dev.marlondvg.book_api.application.port.in;

public record RegisterUserCommand(String email, String password) {

	@Override
	public String toString() {
		return "RegisterUserCommand[email=" + email + ", password=***]";
	}
}
