package dev.marlondvg.book_api.infrastructure.persistence;

import dev.marlondvg.book_api.domain.ReadingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataBookRepository extends JpaRepository<BookJpaEntity, UUID> {

	Optional<BookJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

	List<BookJpaEntity> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

	List<BookJpaEntity> findAllByOwnerIdAndStatusOrderByCreatedAtDesc(UUID ownerId, ReadingStatus status);
}
