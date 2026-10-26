package com.deappie.movies.movie;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

interface MovieRepository extends ReactiveCrudRepository<Movie, Long> {}
