package com.deappie.movies.genre;

import com.deappie.movies.common.BadRequestException;
import com.deappie.movies.common.NotFoundException;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GenreService {
    private final GenreRepository genreRepository;

    Flux<GenreResponse> findAll() {
        return genreRepository.findAll().map(GenreResponse::from);
    }

    Mono<GenreResponse> findById(long id) {
        return genreRepository
                .findById(id)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Genre " + id + " not found")))
                .map(GenreResponse::from);
    }

    public Mono<List<GenreResponse>> findAllByIds(Set<Long> ids) {
        return genreRepository
                .findAllById(ids)
                .map(GenreResponse::from)
                .collectList()
                .flatMap(found -> found.size() == ids.size()
                        ? Mono.just(found)
                        : Mono.error(() -> new BadRequestException("Unknown genre id(s)")));
    }
}
