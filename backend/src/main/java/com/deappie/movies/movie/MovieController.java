package com.deappie.movies.movie;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/movies")
class MovieController {
    private final MovieService movieService;

    MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    Flux<MovieResponse> findAll() {
        return movieService.findAll();
    }

    @GetMapping("/{id}")
    Mono<MovieResponse> findById(@PathVariable long id) {
        return movieService.findById(id);
    }
}
