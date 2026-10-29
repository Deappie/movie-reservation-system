package com.deappie.movies.user;

import com.deappie.movies.auth.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
class UserController {
    private final UserService userService;

    @GetMapping("/me")
    Mono<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        long userId = CurrentUser.id(jwt);
        return userService.findById(userId);
    }
}
