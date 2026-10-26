package com.deappie.movies.movie;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/movies")
class AdminMovieController {
    private final MovieService movieService;

    AdminMovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Mono<MovieResponse> create(@Valid @RequestBody MovieRequest req) {
        return movieService.create(req);
    }

    @PutMapping("/{id}")
    Mono<MovieResponse> update(@PathVariable long id, @Valid @RequestBody MovieRequest req) {
        return movieService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    Mono<Void> delete(@PathVariable long id) {
        return movieService.delete(id);
    }
}
