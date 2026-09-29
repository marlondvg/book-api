package dev.marlondvg.book_api.infrastructure.persistence;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Starts a real PostgreSQL and points the datasource at it. Use it together with
 * {@code @Testcontainers(disabledWithoutDocker = true)} so the test is skipped on
 * machines without Docker; CI always has Docker and fails if these tests are skipped.
 */
@TestConfiguration(proxyBeanMethods = false)
class PostgresTestcontainersConfig {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgres() {
		return new PostgreSQLContainer("postgres:17-alpine");
	}
}
