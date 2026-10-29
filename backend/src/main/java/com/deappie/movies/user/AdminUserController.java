package com.deappie.movies.user;

import jakarta.validation.Valid;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
class AdminUserController {
    private final UserService userService;

    @GetMapping
    Flux<UserResponse> findAll() {
        return userService.findAll();
    }

    @PatchMapping("/{id}/role")
    Mono<UserResponse> changeRole(
            @PathVariable long id, @Valid @RequestBody ChangeRoleRequest req, @AuthenticationPrincipal Jwt jwt) {
        long adminId = Long.parseLong(Objects.requireNonNull(jwt.getSubject()));
        return userService.changeRole(adminId, id, req.role());
    }
}
