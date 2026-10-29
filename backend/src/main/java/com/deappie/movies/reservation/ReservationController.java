package com.deappie.movies.reservation;

import com.deappie.movies.auth.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
class ReservationController {
    private final ReservationService reservationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Mono<ReservationResponse> create(@Valid @RequestBody ReservationRequest req, @AuthenticationPrincipal Jwt jwt) {
        return reservationService.create(CurrentUser.id(jwt), req);
    }

    @GetMapping("/me")
    Flux<MyReservationResponse> findMine(@AuthenticationPrincipal Jwt jwt) {
        return reservationService.findMine(CurrentUser.id(jwt));
    }

    @PostMapping("/{id}/cancel")
    Mono<ReservationResponse> cancel(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return reservationService.cancel(CurrentUser.id(jwt), id);
    }
}
