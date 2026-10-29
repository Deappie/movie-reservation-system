package com.deappie.movies.hall;

import com.deappie.movies.common.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class HallService {
    private final HallRepository hallRepository;

    Flux<HallResponse> findAll() {
        return hallRepository.findAllByOrderByNameAsc().map(HallResponse::from);
    }

    public Mono<Hall> getById(long id) {
        return hallRepository
                .findById(id)
                .switchIfEmpty(Mono.error(() -> new BadRequestException("Unknown hall " + id)));
    }
}
