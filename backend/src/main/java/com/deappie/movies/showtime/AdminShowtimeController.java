package com.deappie.movies.showtime;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/showtimes")
@RequiredArgsConstructor
class AdminShowtimeController {
    private final ShowtimeService showtimeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Mono<ShowtimeResponse> create(@Valid @RequestBody ShowtimeRequest req) {
        return showtimeService.create(req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    Mono<Void> delete(@PathVariable long id) {
        return showtimeService.delete(id);
    }
}
