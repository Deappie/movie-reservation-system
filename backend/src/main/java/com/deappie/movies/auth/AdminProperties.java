package com.deappie.movies.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.admin")
public record AdminProperties(String email, String password) {}
