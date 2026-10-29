package com.deappie.movies.showtime;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("showtimes")
public record Showtime(
        @Id @Nullable Long id, long movieId, long hallId, Instant startsAt, Instant endsAt, BigDecimal basePrice) {

    public long requiredId() {
        return Objects.requireNonNull(id, "Showtime has not been saved yet");
    }
}
