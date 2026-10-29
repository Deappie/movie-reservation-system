package com.deappie.movies.showtime;

import org.jspecify.annotations.Nullable;

import java.util.List;

public record MovieShowtimesResponse(
        long movieId, String title, @Nullable String posterUrl, int durationMin, List<ShowtimeSlot> showtimes) {
}
