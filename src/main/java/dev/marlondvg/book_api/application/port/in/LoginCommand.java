package dev.marlondvg.book_api.application.port.in;

public record LoginCommand(String email, String password, String clientIp) {

	@Override
	public String toString() {
		return "LoginCommand[email=" + email + ", password=***, clientIp=" + clientIp + "]";
	}
}
