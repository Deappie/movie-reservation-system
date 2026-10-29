package com.deappie.movies.showtime;

import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.cinema")
public record CinemaProperties(ZoneId zone, Duration cleaningBuffer) {}
