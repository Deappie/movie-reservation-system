package com.deappie.movies.reservation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

record ReservationRequest(
        @NotNull Long showtimeId, @NotEmpty @Size(max = 10) Set<Long> seatIds) {}
