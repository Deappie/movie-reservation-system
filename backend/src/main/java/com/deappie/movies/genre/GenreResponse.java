package com.deappie.movies.genre;

public record GenreResponse(Long id, String name) {
    static GenreResponse from(Genre genre) {
        return new GenreResponse(genre.id(), genre.name());
    }
}
