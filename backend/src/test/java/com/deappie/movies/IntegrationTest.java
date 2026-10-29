package com.deappie.movies;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
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
    protected static final ZoneId CINEMA_ZONE = ZoneId.of("Europe/Riga");

    private static final AtomicInteger DAY_OFFSET = new AtomicInteger(30);

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

    /** Ids of the first n seeded genres, read from the API so tests never depend on id values. */
    protected List<Long> genreIds(int n) {
        return ids(client.get().uri("/api/genres"), n);
    }

    /** Ids of the seeded halls, sorted by name. */
    protected List<Long> hallIds() {
        return ids(client.get().uri("/api/admin/halls").headers(h -> h.setBearerAuth(adminToken())), Integer.MAX_VALUE);
    }

    /** Creates a movie as admin and returns its id. */
    protected long createMovie(String title, int durationMin, List<Long> genreIds) {
        return createdId(client.post()
                .uri("/api/admin/movies")
                .headers(h -> h.setBearerAuth(adminToken()))
                .bodyValue(Map.of("title", title, "durationMin", durationMin, "genreIds", genreIds)));
    }

    protected long createMovie(String title, List<Long> genreIds) {
        return createMovie(title, 120, genreIds);
    }

    /** Creates a showtime as admin and returns its id. */
    protected long createShowtime(long movieId, long hallId, OffsetDateTime startsAt) {
        return createdId(client.post()
                .uri("/api/admin/showtimes")
                .headers(h -> h.setBearerAuth(adminToken()))
                .bodyValue(Map.of("movieId", movieId, "hallId", hallId, "startsAt", iso(startsAt), "basePrice", 9.50)));
    }

    /**
     * A future day no other test uses, so showtimes created on it never overlap with other tests' showtimes.
     * The test database is fresh for every run, so a simple counter is enough.
     */
    protected static LocalDate uniqueDay() {
        return LocalDate.now(CINEMA_ZONE).plusDays(DAY_OFFSET.incrementAndGet());
    }

    /** Full ISO-8601 timestamp with seconds, e.g. 2026-11-05T18:00:00+02:00 (OffsetDateTime.toString drops ":00"). */
    protected static String iso(OffsetDateTime time) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time);
    }

    /** {@code time} on {@code day}, in the cinema's time zone. */
    protected static OffsetDateTime at(LocalDate day, int hour, int minute) {
        return day.atTime(hour, minute).atZone(CINEMA_ZONE).toOffsetDateTime();
    }

    private List<Long> ids(WebTestClient.RequestHeadersSpec<?> request, int limit) {
        List<Map<String, Object>> items = request.exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult()
                .getResponseBody();
        return Objects.requireNonNull(items).stream()
                .limit(limit)
                .map(item -> ((Number) item.get("id")).longValue())
                .toList();
    }

    private static long createdId(WebTestClient.RequestHeadersSpec<?> request) {
        Map<?, ?> body = request.exchange()
                .expectStatus()
                .isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        return ((Number) Objects.requireNonNull(body).get("id")).longValue();
    }

    private record TokenResponse(String token) {}
}
