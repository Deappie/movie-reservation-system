package com.deappie.movies.genre;

import com.deappie.movies.common.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/genres")
class GenreController {
    private final GenreRepository repository;

    GenreController(GenreRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    Flux<GenreResponse> findAll() {
        return repository.findAll().map(GenreResponse::from);
    }

    @GetMapping("/{id}")
    Mono<GenreResponse> findById(@PathVariable Long id) {
        return repository
                .findById(id)
                .map(GenreResponse::from)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Genre " + id + " not found")));
    }
}
