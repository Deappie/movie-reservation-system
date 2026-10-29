package com.deappie.movies.reservation;

import com.deappie.movies.common.BadRequestException;
import com.deappie.movies.common.ConflictException;
import com.deappie.movies.common.NotFoundException;
import com.deappie.movies.pricing.PriceQuote;
import com.deappie.movies.pricing.PriceRequest;
import com.deappie.movies.pricing.PricingService;
import com.deappie.movies.showtime.Showtime;
import com.deappie.movies.showtime.ShowtimeService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
class ReservationService {
    private final ReservationQueryRepository reservationQueryRepository;
    private final ShowtimeService showtimeService;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final PricingService pricingService;

    Mono<List<SeatResponse>> seatMap(long showtimeId) {
        return reservationQueryRepository
                .findSeatMap(showtimeId)
                .collectList()
                .filter(seats -> !seats.isEmpty())
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Showtime " + showtimeId + " not found")));
    }

    @Transactional
    Mono<ReservationResponse> create(long userId, ReservationRequest req) {
        return showtimeService
                .getById(req.showtimeId())
                .flatMap(showtime -> ensureBookable(showtime, req.seatIds())
                        .then(pricingService.quote(new PriceRequest(
                                userId,
                                showtime.startsAt(),
                                showtime.basePrice(),
                                req.seatIds().size())))
                        .flatMap(quote -> insert(userId, showtime, req.seatIds(), quote)))
                .onErrorMap(
                        DuplicateKeyException.class,
                        _ -> new ConflictException("Some of the selected seats are already taken"))
                .doOnNext(created -> log.info(
                        "User {} reserved seats {} for showtime {}", userId, created.seatIds(), created.showtimeId()));
    }

    private Mono<Void> ensureBookable(Showtime showtime, Set<Long> seatIds) {
        if (!showtime.startsAt().isAfter(Instant.now())) {
            return Mono.error(() -> new BadRequestException("Showtime has already started"));
        }
        return reservationQueryRepository
                .countSeatsInHall(showtime.hallId(), seatIds)
                .flatMap(count -> count == seatIds.size()
                        ? Mono.<Void>empty()
                        : Mono.error(
                                () -> new BadRequestException("Some seats do not belong to this showtime's hall")));
    }

    private Mono<ReservationResponse> insert(long userId, Showtime showtime, Set<Long> seatIds, PriceQuote quote) {
        Reservation reservation = new Reservation(
                null, userId, showtime.requiredId(), ReservationStatus.ACTIVE, quote.total(), null, null);
        return reservationRepository
                .save(reservation)
                .flatMap(saved -> reservationSeatRepository
                        .saveAll(seatIds.stream()
                                .map(seatId -> new ReservationSeat(
                                        null,
                                        saved.requiredId(),
                                        showtime.requiredId(),
                                        seatId,
                                        quote.pricePerSeat(),
                                        true))
                                .toList())
                        .then(Mono.just(ReservationResponse.from(saved, seatIds))));
    }

    Flux<MyReservationResponse> findMine(long userId) {
        return reservationQueryRepository.findByUser(userId, Instant.now());
    }

    @Transactional
    Mono<ReservationResponse> cancel(long userId, long reservationId) {
        return reservationRepository
                .findById(reservationId)
                .filter(reservation -> reservation.userId() == userId)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Reservation " + reservationId + " not found")))
                .flatMap(reservation -> ensureCancellable(reservation)
                        .then(reservationRepository.save(reservation.cancelled(Instant.now())))
                        .flatMap(cancelled -> reservationSeatRepository
                                .deactivateByReservationId(reservationId)
                                .then(seatIdsOf(reservationId))
                                .map(seatIds -> ReservationResponse.from(cancelled, seatIds))))
                .doOnNext(_ -> log.info("User {} cancelled reservation {}", userId, reservationId));
    }

    private Mono<Void> ensureCancellable(Reservation reservation) {
        if (reservation.status() != ReservationStatus.ACTIVE) {
            return Mono.error(() -> new ConflictException("Reservation is already cancelled"));
        }
        return showtimeService
                .getById(reservation.showtimeId())
                .flatMap(showtime -> showtime.startsAt().isAfter(Instant.now())
                        ? Mono.<Void>empty()
                        : Mono.error(() -> new BadRequestException(
                                "Cannot cancel a reservation for a showtime that has already started")));
    }

    private Mono<Set<Long>> seatIdsOf(long reservationId) {
        return reservationSeatRepository
                .findByReservationId(reservationId)
                .map(ReservationSeat::seatId)
                .collect(Collectors.toSet());
    }
}
