package com.deappie.movies.user;

import com.deappie.movies.common.BadRequestException;
import com.deappie.movies.common.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
class UserService {
    private final UserRepository userRepository;

    Flux<UserResponse> findAll() {
        return userRepository.findAll().map(UserResponse::from);
    }

    Mono<UserResponse> changeRole(long actingAdminId, long targetUserId, Role newRole) {
        if (actingAdminId == targetUserId) {
            return Mono.error(() -> new BadRequestException("Admins cannot change their own role"));
        }
        return userRepository
                .findById(targetUserId)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("User " + targetUserId + " not found")))
                .map(user -> user.withRole(newRole))
                .flatMap(userRepository::save)
                .doOnNext(saved -> log.info(
                        "Admin {} changed role of the user {} to {}", actingAdminId, saved.requiredId(), newRole))
                .map(UserResponse::from);
    }

    Mono<UserResponse> findById(long id) {
        return userRepository
                .findById(id)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("User " + id + " not found")))
                .map(UserResponse::from);
    }
}
