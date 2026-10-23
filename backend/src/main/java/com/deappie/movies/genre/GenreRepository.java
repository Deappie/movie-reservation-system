package com.deappie.movies.genre;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

interface GenreRepository extends ReactiveCrudRepository<Genre, Long> {}
