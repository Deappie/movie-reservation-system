package com.deappie.movies.showtime;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
class ShowtimeController {
    private final ShowtimeService showtimeService;
    private final CinemaProperties cinemaProperties;

    @GetMapping
    Flux<MovieShowtimesResponse> findByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate date) {
        LocalDate day = date != null ? date : LocalDate.now(cinemaProperties.zone());
        return showtimeService.findByDate(day);
    }
}
