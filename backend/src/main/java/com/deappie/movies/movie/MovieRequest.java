package com.deappie.movies.movie;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;
import org.jspecify.annotations.Nullable;

record MovieRequest(
        @NotBlank @Size(max = 200) String title,
        @Nullable @Size(max = 5000) String description,
        @Positive @Max(600) int durationMin,
        @NotEmpty Set<Long> genreIds) {}
