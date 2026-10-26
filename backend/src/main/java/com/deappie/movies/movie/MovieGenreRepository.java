package com.deappie.movies.movie;

import java.util.Set;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
class MovieGenreRepository {
    private final DatabaseClient db;

    MovieGenreRepository(DatabaseClient db) {
        this.db = db;
    }

    Mono<Void> addGenres(long movieId, Set<Long> genreIds) {
        return Flux.fromIterable(genreIds)
                .concatMap(
                        genreId -> db.sql("INSERT INTO movie_genres (movie_id, genre_id) VALUES (:movieId, :genreId)")
                                .bind("movieId", movieId)
                                .bind("genreId", genreId)
                                .then())
                .then();
    }

    Mono<Void> removeGenres(long movieId) {
        return db.sql("DELETE FROM movie_genres WHERE movie_id = :movieId")
                .bind("movieId", movieId)
                .then();
    }
}
