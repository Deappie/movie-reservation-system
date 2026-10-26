package com.deappie.movies.genre;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/genres")
class GenreController {
    private final GenreService genreService;

    GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping
    Flux<GenreResponse> findAll() {
        return genreService.findAll();
    }

    @GetMapping("/{id}")
    Mono<GenreResponse> findById(@PathVariable long id) {
        return genreService.findById(id);
    }
}
