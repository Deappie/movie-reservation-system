package com.deappie.movies.movie;

import com.deappie.movies.genre.GenreResponse;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record MovieResponse(
        long id,
        String title,
        @Nullable String description,
        @Nullable String posterUrl,
        int durationMin,
        List<GenreResponse> genres) {
    static MovieResponse from(Movie movie, List<GenreResponse> genres) {
        return new MovieResponse(
                movie.requiredId(), movie.title(), movie.description(), movie.posterUrl(), movie.durationMin(), genres);
    }
}
