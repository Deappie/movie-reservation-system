package com.deappie.movies.user;

public record UserResponse(Long id, String email, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.requiredId(), user.email(), user.role());
    }
}
