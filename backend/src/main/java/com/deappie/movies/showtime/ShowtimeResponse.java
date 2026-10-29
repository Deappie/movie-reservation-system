package com.deappie.movies.showtime;

import java.math.BigDecimal;
import java.time.Instant;

public record ShowtimeResponse(
        long id, long movieId, long hallId, Instant startsAt, Instant endsAt, BigDecimal basePrice) {

    static ShowtimeResponse from(Showtime showtime) {
        return new ShowtimeResponse(
                showtime.requiredId(),
                showtime.movieId(),
                showtime.hallId(),
                showtime.startsAt(),
                showtime.endsAt(),
                showtime.basePrice());
    }
}
