package com.deappie.movies.movie;

import org.jspecify.annotations.Nullable;

record MovieGenreRow(
        long id,
        String title,
        @Nullable String description,
        @Nullable String posterUrl,
        int durationMin,
        @Nullable Long genreId,
        @Nullable String genreName) {}
