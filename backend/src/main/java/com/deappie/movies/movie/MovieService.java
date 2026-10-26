package com.deappie.movies.movie;

import com.deappie.movies.common.ConflictException;
import com.deappie.movies.common.NotFoundException;
import com.deappie.movies.genre.GenreResponse;
import com.deappie.movies.genre.GenreService;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
class MovieService {
    private static final Logger log = LoggerFactory.getLogger(MovieService.class);

    private final MovieRepository movieRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final MovieQueryRepository movieQueryRepository;

    private final GenreService genreService;

    MovieService(
            MovieRepository movieRepository,
            MovieGenreRepository movieGenreRepository,
            MovieQueryRepository movieQueryRepository,
            GenreService genreService) {
        this.movieRepository = movieRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.movieQueryRepository = movieQueryRepository;
        this.genreService = genreService;
    }

    @Transactional
    Mono<MovieResponse> create(MovieRequest req) {
        return genreService
                .findAllByIds(req.genreIds())
                .flatMap(genres -> movieRepository
                        .save(new Movie(
                                null,
                                req.title().trim(),
                                normalizeDescription(req.description()),
                                null,
                                req.durationMin(),
                                null))
                        .flatMap(movie -> movieGenreRepository
                                .addGenres(movie.requiredId(), req.genreIds())
                                .thenReturn(MovieResponse.from(movie, genres))))
                .doOnNext(created -> log.info("Created movie {} '{}'", created.id(), created.title()));
    }

    @Transactional
    Mono<MovieResponse> update(long id, MovieRequest req) {
        return movieRepository.findById(id)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Movie " + id + " not found")))
                .zipWith(genreService.findAllByIds(req.genreIds()))
                .flatMap(tuple -> {
                    Movie existing = tuple.getT1();
                    List<GenreResponse> genres = tuple.getT2();
                    Movie updated = new Movie(existing.id(), req.title().trim(), normalizeDescription(req.description()), existing.posterUrl(), req.durationMin(), existing.createdAt());
                    return movieRepository.save(updated)
                            .flatMap(saved -> movieGenreRepository.removeGenres(id)
                                    .then(movieGenreRepository.addGenres(id, req.genreIds()))
                                    .thenReturn(MovieResponse.from(saved, genres)));
                });
    }

    Mono<Void> delete(long id) {
        return movieRepository.findById(id)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Movie " + id + " not found")))
                .flatMap(movieRepository::delete)
                .onErrorMap(DataIntegrityViolationException.class,
                        _ -> new ConflictException("Movie " + id + " has showtimes and cannot be deleted"))
                .doOnSuccess(_ -> log.info("Deleted movie {}", id));
    }

    private static @Nullable String normalizeDescription(@Nullable String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    Flux<MovieResponse> findAll() {
        return movieQueryRepository
                .findAllWithGenres()
                .bufferUntilChanged(MovieGenreRow::id)
                .map(MovieService::toResponse);
    }

    Mono<MovieResponse> findById(long id) {
        return movieQueryRepository
                .findByIdWithGenres(id)
                .collectList()
                .filter(rows -> !rows.isEmpty())
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Movie " + id + " not found")))
                .map(MovieService::toResponse);
    }

    private static MovieResponse toResponse(List<MovieGenreRow> rows) {
        MovieGenreRow first = rows.getFirst();
        List<GenreResponse> genres = rows.stream()
                .filter(row -> row.genreId() != null)
                .map(row -> new GenreResponse(
                        Objects.requireNonNull(row.genreId()), Objects.requireNonNull(row.genreName())))
                .toList();
        return new MovieResponse(
                first.id(), first.title(), first.description(), first.posterUrl(), first.durationMin(), genres);
    }
}
