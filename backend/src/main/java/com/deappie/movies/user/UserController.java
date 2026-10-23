package com.deappie.movies.user;

import com.deappie.movies.common.NotFoundException;
import java.util.Objects;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
class UserController {
    private final UserRepository userRepository;

    UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    Mono<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(Objects.requireNonNull(jwt.getSubject()));
        return userRepository
                .findById(userId)
                .map(UserResponse::from)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("User not found")));
    }
}
