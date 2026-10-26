package com.deappie.movies.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.deappie.movies.IntegrationTest;
import com.deappie.movies.user.UserResponse;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AuthIntegrationTest extends IntegrationTest {

    @Test
    void signupCreatesUserWithNormalizedEmail() {
        String email = uniqueEmail();
        client.post()
                .uri("/api/auth/signup")
                .bodyValue(new SignupRequest(email.toUpperCase(Locale.ROOT), USER_PASSWORD))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.email")
                .isEqualTo(email)
                .jsonPath("$.role")
                .isEqualTo("USER")
                .jsonPath("$.passwordHash")
                .doesNotExist();
    }

    @Test
    void meRequiresAToken() {
        client.get().uri("/api/users/me").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void duplicateSignupReturns409() {
        String email = uniqueEmail();
        signup(email, USER_PASSWORD);

        client.post()
                .uri("/api/auth/signup")
                .bodyValue(new SignupRequest(email, "anotherpassword"))
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void invalidSignupReturnsFieldErrors() {
        client.post()
                .uri("/api/auth/signup")
                .bodyValue(new SignupRequest("nope", "123"))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.errors.email")
                .exists()
                .jsonPath("$.errors.password")
                .exists();
    }

    @Test
    void loginReturnsToken() {
        String email = uniqueEmail();
        signup(email, USER_PASSWORD);

        String token = login(email, USER_PASSWORD);

        assertThat(token).isNotBlank();
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveSameError() {
        String email = uniqueEmail();
        signup(email, USER_PASSWORD);

        client.post()
                .uri("/api/auth/login")
                .bodyValue(new LoginRequest(email, "wrongpassword"))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Invalid email or password");

        client.post()
                .uri("/api/auth/login")
                .bodyValue(new LoginRequest(uniqueEmail(), USER_PASSWORD))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Invalid email or password");
    }

    @Test
    void meReturnsCurrentUser() {
        String email = uniqueEmail();
        signup(email, USER_PASSWORD);
        String token = login(email, USER_PASSWORD);

        client.get()
                .uri("/api/users/me")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.email")
                .isEqualTo(email)
                .jsonPath("$.role")
                .isEqualTo("USER");
    }

    @Test
    void userCannotAccessAdminEndpoints() {
        String token = userToken();

        client.get()
                .uri("/api/admin/users")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void adminCanListUsers() {
        String adminToken = adminToken();

        client.get()
                .uri("/api/admin/users")
                .headers(h -> h.setBearerAuth(adminToken))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[?(@.email == '" + ADMIN_EMAIL + "')].role")
                .isEqualTo("ADMIN");
    }

    @Test
    void adminCanPromoteAnotherUser() {
        long userId = currentUser(userToken()).id();
        String adminToken = adminToken();

        client.patch()
                .uri("/api/admin/users/{id}/role", userId)
                .headers(h -> h.setBearerAuth(adminToken))
                .bodyValue(Map.of("role", "ADMIN"))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.role")
                .isEqualTo("ADMIN");
    }

    @Test
    void adminCannotChangeOwnRole() {
        String adminToken = adminToken();
        long adminId = currentUser(adminToken).id();

        client.patch()
                .uri("/api/admin/users/{id}/role", adminId)
                .headers(h -> h.setBearerAuth(adminToken))
                .bodyValue(Map.of("role", "USER"))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Admins cannot change their own role");
    }

    private UserResponse currentUser(String token) {
        UserResponse user = client.get()
                .uri("/api/users/me")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserResponse.class)
                .returnResult()
                .getResponseBody();
        return Objects.requireNonNull(user);
    }
}
