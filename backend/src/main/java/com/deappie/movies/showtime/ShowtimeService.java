package com.deappie.movies.showtime;

import com.deappie.movies.common.BadRequestException;
import com.deappie.movies.common.ConflictException;
import com.deappie.movies.common.NotFoundException;
import com.deappie.movies.hall.Hall;
import com.deappie.movies.hall.HallService;
import com.deappie.movies.movie.Movie;
import com.deappie.movies.movie.MovieService;
import io.r2dbc.spi.R2dbcException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeService {
    private static final String HALL_OVERLAP = "23P01";

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeQueryRepository showtimeQueryRepository;
    private final MovieService movieService;
    private final HallService hallService;
    private final CinemaProperties cinemaProperties;

    Mono<ShowtimeResponse> create(ShowtimeRequest req) {
        return Mono.zip(movieService.getById(req.movieId()), hallService.getById(req.hallId()))
                .flatMap(tuple -> {
                    Movie movie = tuple.getT1();
                    Hall hall = tuple.getT2();
                    Instant endsAt = req.startsAt()
                            .plus(Duration.ofMinutes(movie.durationMin()))
                            .plus(cinemaProperties.cleaningBuffer());
                    return showtimeRepository.save(new Showtime(
                            null, movie.requiredId(), hall.requiredId(), req.startsAt(), endsAt, req.basePrice()));
                })
                .onErrorMap(ShowtimeService::isHallOverlap, e -> new ConflictException("Hall is busy at that time"))
                .map(ShowtimeResponse::from)
                .doOnNext(created -> log.info(
                        "Scheduled showtime {} for movie {} in hall {}",
                        created.id(),
                        created.movieId(),
                        created.hallId()));
    }

    Mono<Void> delete(long id) {
        return showtimeRepository
                .findById(id)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Showtime " + id + " not found")))
                .flatMap(showtimeRepository::delete)
                .onErrorMap(
                        DataIntegrityViolationException.class,
                        _ -> new ConflictException("Showtime " + id + " has reservations and cannot be deleted"))
                .doOnSuccess(_ -> log.info("Deleted showtime {}", id));
    }

    private static boolean isHallOverlap(Throwable e) {
        return NestedExceptionUtils.getMostSpecificCause(e) instanceof R2dbcException r2dbc
                && HALL_OVERLAP.equals(r2dbc.getSqlState());
    }

    public Mono<Showtime> getById(long id) {
        return showtimeRepository
                .findById(id)
                .switchIfEmpty(Mono.error(() -> new BadRequestException("Unknown showtime " + id)));
    }

    Flux<MovieShowtimesResponse> findByDate(LocalDate date) {
        ZoneId zone = cinemaProperties.zone();
        Instant from = date.atStartOfDay(zone).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();
        return showtimeQueryRepository
                .findStartingBetween(from, to)
                .bufferUntilChanged(ShowtimeRow::movieId)
                .map(ShowtimeService::toMovieShowtimes);
    }

    private static MovieShowtimesResponse toMovieShowtimes(List<ShowtimeRow> rows) {
        ShowtimeRow first = rows.getFirst();
        List<ShowtimeSlot> slots = rows.stream()
                .map(r -> new ShowtimeSlot(
                        r.id(), r.hallId(), r.hallName(), r.startsAt(), r.endsAt(), r.basePrice(), r.seatsLeft()))
                .toList();
        return new MovieShowtimesResponse(
                first.movieId(), first.title(), first.posterUrl(), first.durationMin(), slots);
    }
}
