package com.deappie.movies.showtime;

import java.util.List;
import org.jspecify.annotations.Nullable;

public record MovieShowtimesResponse(
        long movieId, String title, @Nullable String posterUrl, int durationMin, List<ShowtimeSlot> showtimes) {}
