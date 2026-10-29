package com.deappie.movies.reservation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Table;

@Table("reservations")
public record Reservation(
        @Id @Nullable Long id,
        long userId,
        long showtimeId,
        ReservationStatus status,
        BigDecimal totalPrice,
        @ReadOnlyProperty @Nullable Instant createdAt,
        @Nullable Instant cancelledAt) {

    public long requiredId() {
        return Objects.requireNonNull(id, "Reservation has not been saved yet");
    }

    /** A copy of this reservation, cancelled at the given moment. */
    public Reservation cancelled(Instant at) {
        return new Reservation(id, userId, showtimeId, ReservationStatus.CANCELLED, totalPrice, createdAt, at);
    }
}
