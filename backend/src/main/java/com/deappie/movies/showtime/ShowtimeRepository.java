package com.deappie.movies.showtime;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

interface ShowtimeRepository extends ReactiveCrudRepository<Showtime, Long> {}
