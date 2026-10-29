package com.deappie.movies.reservation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record MyReservationResponse(
        long id,
        ReservationStatus status,
        BigDecimal totalPrice,
        long showtimeId,
        Instant startsAt,
        long movieId,
        String movieTitle,
        @Nullable String posterUrl,
        String hallName,
        List<String> seats,
        boolean cancellable,
        @Nullable Instant createdAt,
        @Nullable Instant cancelledAt) {}
