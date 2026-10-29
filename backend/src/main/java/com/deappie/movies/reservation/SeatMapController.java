package com.deappie.movies.reservation;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
class SeatMapController {
    private final ReservationService reservationService;

    @GetMapping("/api/showtimes/{showtimeId}/seats")
    Mono<List<SeatResponse>> seatMap(@PathVariable long showtimeId) {
        return reservationService.seatMap(showtimeId);
    }
}
