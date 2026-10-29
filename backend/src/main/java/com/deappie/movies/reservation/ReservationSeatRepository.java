package com.deappie.movies.reservation;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface ReservationSeatRepository extends ReactiveCrudRepository<ReservationSeat, Long> {

    @Modifying
    @Query("UPDATE reservation_seats SET active = false WHERE reservation_id = :reservationId")
    Mono<Integer> deactivateByReservationId(long reservationId);

    Flux<ReservationSeat> findByReservationId(long reservationId);
}
