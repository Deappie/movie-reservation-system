package com.deappie.movies.auth;

import jakarta.validation.constraints.NotBlank;

record LoginRequest(@NotBlank String email, @NotBlank String password) {}
