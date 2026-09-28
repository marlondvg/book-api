CREATE TABLE users (
                       id            UUID PRIMARY KEY,
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE books (
                       id          UUID PRIMARY KEY,
                       owner_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                       title       VARCHAR(255) NOT NULL,
                       author      VARCHAR(255) NOT NULL,
                       pages       INTEGER CHECK (pages > 0),
                       isbn        VARCHAR(20),
                       cover_url   VARCHAR(500),
                       status      VARCHAR(20) NOT NULL
                           CHECK (status IN ('TO_READ', 'READING', 'READ', 'ABANDONED')),
                       rating      SMALLINT CHECK (rating BETWEEN 1 AND 5),
                       started_at  DATE,
                       finished_at DATE,
                       created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
                       CONSTRAINT rating_only_when_finished
                           CHECK (rating IS NULL OR status IN ('READ', 'ABANDONED'))
);

CREATE INDEX idx_books_owner_status ON books (owner_id, status);