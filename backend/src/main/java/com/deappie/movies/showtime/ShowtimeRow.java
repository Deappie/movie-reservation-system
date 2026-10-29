package com.deappie.movies.showtime;

import java.math.BigDecimal;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

record ShowtimeRow(
        long id,
        Instant startsAt,
        Instant endsAt,
        BigDecimal basePrice,
        long movieId,
        String title,
        @Nullable String posterUrl,
        int durationMin,
        long hallId,
        String hallName,
        long seatsLeft) {}
