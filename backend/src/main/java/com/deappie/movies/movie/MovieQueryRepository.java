package com.deappie.movies.movie;

import io.r2dbc.spi.Readable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    Mono<Long> count(MovieFilter filter) {
        Where where = where(filter);
        DatabaseClient.GenericExecuteSpec spec = db.sql("SELECT count(*) FROM movies m" + where.sql());
        for (var param : where.params().entrySet()) {
            spec = spec.bind(param.getKey(), param.getValue());
        }
        return spec.map(row -> Objects.requireNonNull(row.get(0, Long.class))).one();
    }

    Flux<MovieGenreRow> findPageWithGenres(MovieFilter filter, int page, int size) {
        Where where = where(filter);
        String sql = """
                WITH page AS (
                    SELECT m.* FROM movies m %s
                    ORDER BY m.title, m.id
                    LIMIT :limit OFFSET :offset
                )
                SELECT p.id, p.title, p.description, p.poster_url, p.duration_min,
                    g.id AS genre_id, g.name AS genre_name
                FROM page p
                LEFT JOIN movie_genres mg ON mg.movie_id = p.id
                LEFT JOIN genres g ON g.id = mg.genre_id
                ORDER BY p.title, p.id, g.name
                """.formatted(where.sql());
        DatabaseClient.GenericExecuteSpec spec = db.sql(sql).bind("limit", size).bind("offset", (long) page * size);
        for (var param : where.params().entrySet()) {
            spec = spec.bind(param.getKey(), param.getValue());
        }
        return spec.map(MovieQueryRepository::toRow).all();
    }

    private record Where(String sql, Map<String, Object> params) {}

    private static Where where(MovieFilter filter) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        String q = filter.q();
        if (q != null && !q.isBlank()) {
            conditions.add("m.title ILIKE :q");
            params.put("q", "%" + q.trim() + "%");
        }
        Long genreId = filter.genreId();
        if (genreId != null) {
            conditions.add("EXISTS (SELECT 1 FROM movie_genres f WHERE f.movie_id = m.id AND f.genre_id = :genreId)");
            params.put("genreId", genreId);
        }
        String sql = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        return new Where(sql, params);
    }
}
