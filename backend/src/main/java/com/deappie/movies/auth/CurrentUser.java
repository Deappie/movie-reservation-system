package com.deappie.movies.auth;

import java.util.Objects;
import org.springframework.security.oauth2.jwt.Jwt;

/** Reads who is calling from the verified JWT. */
public final class CurrentUser {

    private CurrentUser() {}

    /** The id of the logged-in user, taken from the token's {@code sub} claim set at login. */
    public static long id(Jwt jwt) {
        return Long.parseLong(Objects.requireNonNull(jwt.getSubject(), "JWT has no subject"));
    }
}
