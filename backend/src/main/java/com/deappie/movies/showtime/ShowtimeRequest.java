package com.deappie.movies.showtime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

record ShowtimeRequest(
        @NotNull Long movieId,
        @NotNull Long hallId,
        @NotNull @Future Instant startsAt,

        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2)
        BigDecimal basePrice) {}
