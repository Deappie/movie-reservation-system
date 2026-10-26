package com.deappie.movies.movie;

import io.r2dbc.spi.Readable;
import java.util.Objects;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
class MovieQueryRepository {
    private static final String SELECT_WITH_GENRES = """
            SELECT m.id, m.title, m.description, m.poster_url, m.duration_min,
                g.id AS genre_id, g.name AS genre_name
            FROM movies m
            LEFT JOIN movie_genres mg ON mg.movie_id = m.id
            LEFT JOIN genres g ON g.id = mg.genre_id
            """;

    private final DatabaseClient db;

    MovieQueryRepository(DatabaseClient db) {
        this.db = db;
    }

    Flux<MovieGenreRow> findAllWithGenres() {
        return db.sql(SELECT_WITH_GENRES + " ORDER BY m.title, m.id, g.name")
                .map(MovieQueryRepository::toRow)
                .all();
    }

    Flux<MovieGenreRow> findByIdWithGenres(long id) {
        return db.sql(SELECT_WITH_GENRES + " WHERE m.id = :id ORDER BY g.name")
                .bind("id", id)
                .map(MovieQueryRepository::toRow)
                .all();
    }

    private static MovieGenreRow toRow(Readable row) {
        return new MovieGenreRow(
                Objects.requireNonNull(row.get("id", Long.class)),
                Objects.requireNonNull(row.get("title", String.class)),
                row.get("description", String.class),
                row.get("poster_url", String.class),
                Objects.requireNonNull(row.get("duration_min", Integer.class)),
                row.get("genre_id", Long.class),
                row.get("genre_name", String.class));
    }
}
