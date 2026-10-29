package com.deappie.movies.showtime;

import io.r2dbc.spi.Readable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
@RequiredArgsConstructor
class ShowtimeQueryRepository {
    private final DatabaseClient db;

    Flux<ShowtimeRow> findStartingBetween(Instant from, Instant to) {
        return db.sql("""
                SELECT s.id, s.starts_at, s.ends_at, s.base_price,
                       m.id AS movie_id, m.title, m.poster_url, m.duration_min,
                       h.id AS hall_id, h.name AS hall_name,
                       h.rows * h.seats_per_row
                         - (SELECT count(*) FROM reservation_seats rs
                            WHERE rs.showtime_id = s.id AND rs.active) AS seats_left
                FROM showtimes s
                JOIN movies m ON m.id = s.movie_id
                JOIN halls h ON h.id = s.hall_id
                WHERE s.starts_at >= :from AND s.starts_at < :to
                ORDER BY m.title, m.id, s.starts_at
                """)
                .bind("from", from)
                .bind("to", to)
                .map(ShowtimeQueryRepository::toRow)
                .all();
    }

    private static ShowtimeRow toRow(Readable row) {
        return new ShowtimeRow(
                Objects.requireNonNull(row.get("id", Long.class)),
                Objects.requireNonNull(row.get("starts_at", Instant.class)),
                Objects.requireNonNull(row.get("ends_at", Instant.class)),
                Objects.requireNonNull(row.get("base_price", BigDecimal.class)),
                Objects.requireNonNull(row.get("movie_id", Long.class)),
                Objects.requireNonNull(row.get("title", String.class)),
                row.get("poster_url", String.class),
                Objects.requireNonNull(row.get("duration_min", Integer.class)),
                Objects.requireNonNull(row.get("hall_id", Long.class)),
                Objects.requireNonNull(row.get("hall_name", String.class)),
                Objects.requireNonNull(row.get("seats_left", Long.class)));
    }
}
