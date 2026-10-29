package com.deappie.movies.hall;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

interface HallRepository extends ReactiveCrudRepository<Hall, Long> {
    Flux<Hall> findAllByOrderByNameAsc();
}
