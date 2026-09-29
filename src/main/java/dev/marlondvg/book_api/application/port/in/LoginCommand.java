package dev.marlondvg.book_api.application.port.in;

public record LoginCommand(String email, String password) {

	@Override
	public String toString() {
		return "LoginCommand[email=" + email + ", password=***]";
	}
}
