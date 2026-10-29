package com.deappie.movies.movie;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/movies")
@RequiredArgsConstructor
class AdminMovieController {
    private final MovieService movieService;

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

    @PutMapping(value = "/{id}/poster", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Mono<MovieResponse> updatePoster(@PathVariable long id, @RequestPart("file") FilePart file) {
        return movieService.updatePoster(id, file);
    }
}
