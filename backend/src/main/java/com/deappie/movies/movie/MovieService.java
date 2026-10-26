package com.deappie.movies.movie;

import com.deappie.movies.common.ConflictException;
import com.deappie.movies.common.NotFoundException;
import com.deappie.movies.common.PageResponse;
import com.deappie.movies.genre.GenreResponse;
import com.deappie.movies.genre.GenreService;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
class MovieService {
    private static final Logger log = LoggerFactory.getLogger(MovieService.class);

    private final MovieRepository movieRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final MovieQueryRepository movieQueryRepository;

    private final GenreService genreService;

    private final PosterStorage posterStorage;

    MovieService(
            MovieRepository movieRepository,
            MovieGenreRepository movieGenreRepository,
            MovieQueryRepository movieQueryRepository,
            GenreService genreService,
            PosterStorage posterStorage) {
        this.movieRepository = movieRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.movieQueryRepository = movieQueryRepository;
        this.genreService = genreService;
        this.posterStorage = posterStorage;
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
        return movieRepository
                .findById(id)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Movie " + id + " not found")))
                .zipWith(genreService.findAllByIds(req.genreIds()))
                .flatMap(tuple -> {
                    Movie existing = tuple.getT1();
                    List<GenreResponse> genres = tuple.getT2();
                    Movie updated = new Movie(
                            existing.id(),
                            req.title().trim(),
                            normalizeDescription(req.description()),
                            existing.posterUrl(),
                            req.durationMin(),
                            existing.createdAt());
                    return movieRepository
                            .save(updated)
                            .flatMap(saved -> movieGenreRepository
                                    .removeGenres(id)
                                    .then(movieGenreRepository.addGenres(id, req.genreIds()))
                                    .thenReturn(MovieResponse.from(saved, genres)));
                });
    }

    Mono<Void> delete(long id) {
        return movieRepository
                .findById(id)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Movie " + id + " not found")))
                .flatMap(movieRepository::delete)
                .onErrorMap(
                        DataIntegrityViolationException.class,
                        _ -> new ConflictException("Movie " + id + " has showtimes and cannot be deleted"))
                .doOnSuccess(_ -> log.info("Deleted movie {}", id));
    }

    Mono<MovieResponse> updatePoster(long id, FilePart file) {
        return movieRepository
                .findById(id)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Movie " + id + " not found")))
                .flatMap(existing -> posterStorage
                        .store(file)
                        .flatMap(url -> movieRepository.save(existing.withPosterUrl(url)))
                        .then(posterStorage.delete(existing.posterUrl()).onErrorResume(e -> {
                            log.warn("Could not delete old poster {} of movie {}", existing.posterUrl(), id, e);
                            return Mono.empty();
                        })))
                .then(findById(id))
                .doOnNext(_ -> log.info("Updated poster of movie {}", id));
    }

    private static @Nullable String normalizeDescription(@Nullable String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    Mono<PageResponse<MovieResponse>> findPage(MovieFilter filter, int page, int size) {
        Mono<List<MovieResponse>> items = movieQueryRepository
                .findPageWithGenres(filter, page, size)
                .bufferUntilChanged(MovieGenreRow::id)
                .map(MovieService::toResponse)
                .collectList();
        return Mono.zip(items, movieQueryRepository.count(filter))
                .map(tuple -> PageResponse.of(tuple.getT1(), page, size, tuple.getT2()));
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
