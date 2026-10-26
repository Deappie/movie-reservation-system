package com.deappie.movies;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@Import(TestcontainersConfiguration.class)
@AutoConfigureWebTestClient
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.admin.email=" + IntegrationTest.ADMIN_EMAIL,
            "app.admin.password=" + IntegrationTest.ADMIN_PASSWORD,
            "app.posters.dir=target/test-posters"
        })
public abstract class IntegrationTest {
    protected static final String ADMIN_EMAIL = "admin@test.local";
    protected static final String ADMIN_PASSWORD = "admin-password";
    protected static final String USER_PASSWORD = "supersecret";

    @Autowired
    protected WebTestClient client;

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.local";
    }

    protected void signup(String email, String password) {
        client.post()
                .uri("/api/auth/signup")
                .bodyValue(Map.of("email", email, "password", password))
                .exchange()
                .expectStatus()
                .isCreated();
    }

    protected String login(String email, String password) {
        TokenResponse response = client.post()
                .uri("/api/auth/login")
                .bodyValue(Map.of("email", email, "password", password))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TokenResponse.class)
                .returnResult()
                .getResponseBody();
        return Objects.requireNonNull(response).token();
    }

    protected String adminToken() {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected String userToken() {
        String email = uniqueEmail();
        signup(email, USER_PASSWORD);
        return login(email, USER_PASSWORD);
    }

    private record TokenResponse(String token) {}
}
