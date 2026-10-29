package com.deappie.movies.auth;

import com.deappie.movies.user.Role;
import com.deappie.movies.user.User;
import com.deappie.movies.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
class AdminSeeder implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    public void run(ApplicationArguments args) {
        String email = User.normalizeEmail(adminProperties.email());
        userRepository
                .existsByEmail(email)
                .flatMap(exists -> {
                    if (exists) {
                        log.info("Admin user already present");
                        return Mono.<User>empty();
                    }
                    String hash = passwordEncoder.encode(adminProperties.password());
                    return userRepository
                            .save(new User(null, email, hash, Role.ADMIN, null))
                            .doOnNext(admin -> log.info("Seeded admin user with id {}", admin.requiredId()));
                })
                .block();
    }
}
