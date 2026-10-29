package com.deappie.movies.auth;

import com.deappie.movies.common.ConflictException;
import com.deappie.movies.common.UnauthorizedException;
import com.deappie.movies.user.Role;
import com.deappie.movies.user.User;
import com.deappie.movies.user.UserRepository;
import com.deappie.movies.user.UserResponse;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final JwtEncoder jwtEncoder;

    Mono<UserResponse> signup(SignupRequest req) {
        String email = User.normalizeEmail(req.email());
        return userRepository
                .existsByEmail(email)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ConflictException("Email already registered"));
                    }
                    return Mono.fromCallable(() -> passwordEncoder.encode(req.password()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(hash -> userRepository.save(new User(null, email, hash, Role.USER, null)));
                })
                .map(UserResponse::from);
    }

    Mono<LoginResponse> login(LoginRequest req) {
        String email = User.normalizeEmail(req.email());
        return userRepository
                .findByEmail(email)
                .switchIfEmpty(Mono.error(() -> new UnauthorizedException("Invalid email or password")))
                .flatMap(user -> Mono.fromCallable(() -> passwordEncoder.matches(req.password(), user.passwordHash()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(matches -> {
                            if (!matches) {
                                return Mono.error(new UnauthorizedException("Invalid email or password"));
                            }
                            return Mono.just(issueToken(user));
                        }));
    }

    private LoginResponse issueToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.ttl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("movie-reservation")
                .subject(String.valueOf(user.requiredId()))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("roles", List.of(user.role().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token =
                jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(token, expiresAt);
    }
}
