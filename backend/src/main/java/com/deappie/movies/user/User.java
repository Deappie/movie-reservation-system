package com.deappie.movies.user;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public record User(
        @Id @Nullable Long id,
        String email,
        String passwordHash,
        Role role,
        @ReadOnlyProperty @Nullable Instant createdAt) {

    public Long requiredId() {
        return Objects.requireNonNull(id, "User has not been saved yet");
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
