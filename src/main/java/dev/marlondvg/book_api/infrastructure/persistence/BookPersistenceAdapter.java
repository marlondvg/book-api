package dev.marlondvg.book_api.infrastructure.persistence;

import dev.marlondvg.book_api.application.port.out.BookRepository;
import dev.marlondvg.book_api.domain.Book;
import dev.marlondvg.book_api.domain.ReadingStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class BookPersistenceAdapter implements BookRepository {

	private final SpringDataBookRepository repository;

	BookPersistenceAdapter(SpringDataBookRepository repository) {
		this.repository = repository;
	}

	@Override
	public Book save(Book book) {
		return BookMapper.toDomain(repository.save(BookMapper.toEntity(book)));
	}

	@Override
	public Optional<Book> findByIdAndOwnerId(UUID id, UUID ownerId) {
		return repository.findByIdAndOwnerId(id, ownerId).map(BookMapper::toDomain);
	}

	@Override
	public List<Book> findAllByOwnerId(UUID ownerId, ReadingStatus status) {
		List<BookJpaEntity> entities = status == null
				? repository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)
				: repository.findAllByOwnerIdAndStatusOrderByCreatedAtDesc(ownerId, status);
		return entities.stream().map(BookMapper::toDomain).toList();
	}

	@Override
	public void delete(Book book) {
		repository.deleteById(book.getId());
	}
}
