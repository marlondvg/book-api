package dev.marlondvg.book_api.infrastructure.persistence;

import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs every {@link BookPersistenceAdapterTest} case against PostgreSQL.
 */
@Testcontainers(disabledWithoutDocker = true)
@Import({BookPersistenceAdapter.class, UserPersistenceAdapter.class, PostgresTestcontainersConfig.class})
class BookPersistenceAdapterPostgresTest extends BookPersistenceAdapterTest {
}
