package com.deappie.movies.auth;

import java.time.Instant;

record LoginResponse(String token, Instant expiresAt) {}
