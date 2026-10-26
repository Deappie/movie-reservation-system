package com.deappie.movies.movie;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.posters")
public record PosterProperties(String dir) {}
