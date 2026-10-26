package com.deappie.movies.movie;

import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Table;

@Table("movies")
public record Movie(
        @Id @Nullable Long id,
        String title,
        @Nullable String description,
        @Nullable String posterUrl,
        int durationMin,
        @ReadOnlyProperty @Nullable Instant createdAt) {

    public long requiredId() {
        return Objects.requireNonNull(id, "Movie has not been saved yet");
    }

    public Movie withPosterUrl(String posterUrl) {
        return new Movie(id, title, description, posterUrl, durationMin, createdAt);
    }
}
