package dev.marlondvg.book_api.domain;

public enum ReadingStatus {
	TO_READ,
	READING,
	READ,
	ABANDONED;

	public boolean canTransitionTo(ReadingStatus target) {
		return switch (this) {
			case TO_READ, READING -> true;
			case READ -> target == READ || target == READING;
			case ABANDONED -> target != READ;
		};
	}

	public boolean allowsRating() {
		return this == READ || this == ABANDONED;
	}
}
