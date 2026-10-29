package com.deappie.movies.showtime;

import java.math.BigDecimal;
import java.time.Instant;

public record ShowtimeSlot(
        long id, long hallId, String hallName, Instant startsAt, Instant endsAt, BigDecimal basePrice, long seatsLeft) {
}
