package com.deappie.movies.movie;

import com.deappie.movies.common.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
class MovieController {
    private final MovieService movieService;

    @GetMapping
    Mono<PageResponse<MovieResponse>> findPage(
            @RequestParam(required = false) @Nullable String q,
            @RequestParam(required = false) @Nullable Long genreId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return movieService.findPage(new MovieFilter(q, genreId), page, size);
    }

    @GetMapping("/{id}")
    Mono<MovieResponse> findById(@PathVariable long id) {
        return movieService.findById(id);
    }
}
