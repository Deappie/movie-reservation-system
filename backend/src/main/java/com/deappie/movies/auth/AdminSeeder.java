package com.deappie.movies.auth;

import com.deappie.movies.user.Role;
import com.deappie.movies.user.User;
import com.deappie.movies.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
class AdminSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    AdminSeeder(UserRepository users, PasswordEncoder passwordEncoder, AdminProperties adminProperties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = User.normalizeEmail(adminProperties.email());
        users.existsByEmail(email)
                .flatMap(exists -> {
                    if (exists) {
                        log.info("Admin user already present");
                        return Mono.<User>empty();
                    }
                    String hash = passwordEncoder.encode(adminProperties.password());
                    return users.save(new User(null, email, hash, Role.ADMIN, null))
                            .doOnNext(admin -> log.info("Seeded admin user with id {}", admin.requiredId()));
                })
                .block();
    }
}
