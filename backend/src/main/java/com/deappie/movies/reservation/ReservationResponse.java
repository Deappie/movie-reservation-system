package com.deappie.movies.reservation;

import java.math.BigDecimal;
import java.util.Set;

public record ReservationResponse(
        long id, long showtimeId, ReservationStatus status, BigDecimal totalPrice, Set<Long> seatIds) {

    static ReservationResponse from(Reservation reservation, Set<Long> seatIds) {
        return new ReservationResponse(
                reservation.requiredId(),
                reservation.showtimeId(),
                reservation.status(),
                reservation.totalPrice(),
                seatIds);
    }
}
