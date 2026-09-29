package dev.marlondvg.book_api.application.port.out;

public interface PasswordHasher {

	String hash(String rawPassword);

	boolean matches(String rawPassword, String passwordHash);
}
