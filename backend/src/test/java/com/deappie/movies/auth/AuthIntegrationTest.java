package com.deappie.movies.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.deappie.movies.TestcontainersConfiguration;
import com.deappie.movies.user.UserResponse;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

@Import(TestcontainersConfiguration.class)
@AutoConfigureWebTestClient
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.admin.email=" + AuthIntegrationTest.ADMIN_EMAIL,
            "app.admin.password=" + AuthIntegrationTest.ADMIN_PASSWORD
        })
class AuthIntegrationTest {

    static final String ADMIN_EMAIL = "admin@test.local";
    static final String ADMIN_PASSWORD = "admin-password";

    @Autowired
    WebTestClient client;

    // -- helpers --

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.local";
    }

    private void signup(String email, String password) {
        client.post()
                .uri("/api/auth/signup")
                .bodyValue(new SignupRequest(email, password))
                .exchange()
                .expectStatus()
                .isCreated();
    }

    private String login(String email, String password) {
        LoginResponse response = client.post()
                .uri("/api/auth/login")
                .bodyValue(new LoginRequest(email, password))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();
        return Objects.requireNonNull(response).token();
    }

    // -- tests --

    @Test
    void signupCreatesUserWithNormalizedEmail() {
        String email = uniqueEmail();
        client.post()
                .uri("/api/auth/signup")
                .bodyValue(new SignupRequest(email.toUpperCase(Locale.ROOT), "supersecret"))
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
        signup(email, "supersecret");

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
        signup(email, "supersecret");

        String token = login(email, "supersecret");

        assertThat(token).isNotBlank();
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveSameError() {
        String email = uniqueEmail();
        signup(email, "supersecret");

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
                .bodyValue(new LoginRequest(uniqueEmail(), "supersecret"))
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
        signup(email, "supersecret");
        String token = login(email, "supersecret");

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
        String email = uniqueEmail();
        signup(email, "supersecret");
        String token = login(email, "supersecret");

        client.get()
                .uri("/api/admin/users")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void adminCanListUsers() {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

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
        String email = uniqueEmail();
        signup(email, "supersecret");
        long userId = currentUser(login(email, "supersecret")).id();
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

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
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
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
