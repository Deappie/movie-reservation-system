package com.deappie.movies.reservation;

import io.r2dbc.spi.Readable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
class ReservationQueryRepository {
    private final DatabaseClient db;

    Flux<SeatResponse> findSeatMap(long showtimeId) {
        return db.sql("""
                        SELECT se.id, se.row_label, se.number, (rs.id IS NOT NULL) AS taken
                        FROM showtimes S
                        JOIN seats se ON se.hall_id = s.hall_id
                        LEFT JOIN reservation_seats rs
                            ON rs.seat_id = se.id AND rs.showtime_id = s.id AND rs.active
                        WHERE s.id = :showtimeId
                        ORDER BY se.row_label, se.number
                        """)
                .bind("showtimeId", showtimeId)
                .map(row -> new SeatResponse(
                        Objects.requireNonNull(row.get("id", Long.class)),
                        Objects.requireNonNull(row.get("row_label", String.class)),
                        Objects.requireNonNull(row.get("number", Integer.class)),
                        Objects.requireNonNull(row.get("taken", Boolean.class))))
                .all();
    }

    Mono<Long> countSeatsInHall(long hallId, Set<Long> seatIds) {
        return db.sql("SELECT count(*) FROM seats WHERE hall_id = :hallId AND id = ANY(:seatIds)")
                .bind("hallId", hallId)
                .bind("seatIds", seatIds.toArray(Long[]::new))
                .map(row -> Objects.requireNonNull(row.get(0, Long.class)))
                .one();
    }

    Flux<MyReservationResponse> findByUser(long userId, Instant now) {
        return db.sql("""
                    SELECT r.id, r.status, r.total_price, r.created_at, r.cancelled_at,
                           s.id AS showtime_id, s.starts_at,
                           m.id AS movie_id, m.title AS movie_title, m.poster_url,
                           h.name AS hall_name,
                           array_agg(se.row_label || se.number ORDER BY se.row_label, se.number) AS seats
                    FROM reservations r
                    JOIN showtimes s ON s.id = r.showtime_id
                    JOIN movies m ON m.id = s.movie_id
                    JOIN halls h ON h.id = s.hall_id
                    JOIN reservation_seats rs ON rs.reservation_id = r.id
                    JOIN seats se ON se.id = rs.seat_id
                    WHERE r.user_id = :userId
                    GROUP BY r.id, s.id, m.id, h.id
                    ORDER BY s.starts_at DESC
                    """)
                .bind("userId", userId)
                .map(row -> toMyReservation(row, now))
                .all();
    }

    private static MyReservationResponse toMyReservation(Readable row, Instant now) {
        ReservationStatus status = ReservationStatus.valueOf(Objects.requireNonNull(row.get("status", String.class)));
        Instant startsAt = Objects.requireNonNull(row.get("starts_at", Instant.class));
        return new MyReservationResponse(
                Objects.requireNonNull(row.get("id", Long.class)),
                status,
                Objects.requireNonNull(row.get("total_price", BigDecimal.class)),
                Objects.requireNonNull(row.get("showtime_id", Long.class)),
                startsAt,
                Objects.requireNonNull(row.get("movie_id", Long.class)),
                Objects.requireNonNull(row.get("movie_title", String.class)),
                row.get("poster_url", String.class),
                Objects.requireNonNull(row.get("hall_name", String.class)),
                List.of(Objects.requireNonNull(row.get("seats", String[].class))),
                status == ReservationStatus.ACTIVE && startsAt.isAfter(now),
                row.get("created_at", Instant.class),
                row.get("cancelled_at", Instant.class));
    }
}
