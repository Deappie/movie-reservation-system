package com.deappie.movies.user;

import com.deappie.movies.common.BadRequestException;
import com.deappie.movies.common.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository users;

    UserService(UserRepository users) {
        this.users = users;
    }

    Flux<UserResponse> findAll() {
        return users.findAll().map(UserResponse::from);
    }

    Mono<UserResponse> changeRole(long actingAdminId, long targetUserId, Role newRole) {
        if (actingAdminId == targetUserId) {
            return Mono.error(() -> new BadRequestException("Admins cannot change their own role"));
        }
        return users.findById(targetUserId)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("User " + targetUserId + " not found")))
                .map(user -> user.withRole(newRole))
                .flatMap(users::save)
                .doOnNext(saved -> log.info(
                        "Admin {} changed role of the user {} to {}", actingAdminId, saved.requiredId(), newRole))
                .map(UserResponse::from);
    }

    Mono<UserResponse> findById(long id) {
        return users.findById(id)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("User " + id + " not found")))
                .map(UserResponse::from);
    }
}
