package com.deappie.movies.hall;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("halls")
public record Hall(@Id @Nullable Long id, String name, int rows, int seatsPerRow) {
    public long requiredId() {
        return Objects.requireNonNull(id, "Hall has not been saved yet");
    }

    public int capacity() {
        return rows * seatsPerRow;
    }
}
