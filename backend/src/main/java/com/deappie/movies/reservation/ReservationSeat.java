package com.deappie.movies.reservation;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("reservation_seats")
public record ReservationSeat(
        @Id @Nullable Long id, long reservationId, long showtimeId, long seatId, BigDecimal price, boolean active) {}
