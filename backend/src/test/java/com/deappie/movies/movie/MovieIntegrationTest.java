package com.deappie.movies.movie;

import com.deappie.movies.IntegrationTest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

class MovieIntegrationTest extends IntegrationTest {

    // --- create ---

    @Test
    void adminCreatesMovieWithGenres() {
        List<Long> genres = genreIds(2);

        client.post()
                .uri("/api/admin/movies")
                .headers(h -> h.setBearerAuth(adminToken()))
                .bodyValue(Map.of("title", "  Inception ", "durationMin", 148, "genreIds", genres))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Inception")
                .jsonPath("$.genres.length()")
                .isEqualTo(2)
                .jsonPath("$.posterUrl")
                .doesNotExist();
    }

    @Test
    void unknownGenreIsRejected() {
        client.post()
                .uri("/api/admin/movies")
                .headers(h -> h.setBearerAuth(adminToken()))
                .bodyValue(Map.of("title", "Ghost genre", "durationMin", 100, "genreIds", List.of(999_999L)))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Unknown genre id(s)");
    }

    @Test
    void invalidMovieReturnsFieldErrors() {
        client.post()
                .uri("/api/admin/movies")
                .headers(h -> h.setBearerAuth(adminToken()))
                .bodyValue(Map.of("title", "", "durationMin", 0, "genreIds", genreIds(1)))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.errors.title")
                .exists()
                .jsonPath("$.errors.durationMin")
                .exists();
    }

    // --- access ---

    @Test
    void userCannotCreateMovies() {
        String token = userToken();

        client.post()
                .uri("/api/admin/movies")
                .headers(h -> h.setBearerAuth(token))
                .bodyValue(Map.of("title", "Nope", "durationMin", 90, "genreIds", genreIds(1)))
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void anonymousCanBrowseButNotCreate() {
        client.get().uri("/api/movies").exchange().expectStatus().isOk();

        client.post()
                .uri("/api/admin/movies")
                .bodyValue(Map.of("title", "Nope", "durationMin", 90, "genreIds", genreIds(1)))
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    // --- read ---

    @Test
    void unknownMovieReturns404() {
        client.get()
                .uri("/api/movies/999999")
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Movie 999999 not found");
    }

    @Test
    void pageSizeIsLimited() {
        client.get().uri("/api/movies?size=500").exchange().expectStatus().isBadRequest();
    }

    @Test
    void genreFilterKeepsAllGenres() {
        List<Long> genres = genreIds(2);
        long id = createMovie("Filter " + UUID.randomUUID(), genres);

        client.get()
                .uri("/api/movies?genreId={genreId}&size=100", genres.getFirst())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.items[?(@.id == " + id + ")]")
                .exists()
                .jsonPath("$.items[?(@.id == " + id + ")].genres.length()")
                .isEqualTo(2);
    }

    @Test
    void searchIgnoresCaseAndTrimsInput() {
        String title = "Case Test " + UUID.randomUUID();
        long id = createMovie(title, genreIds(1));

        client.get()
                .uri(uri -> uri.path("/api/movies")
                        .queryParam("q", "  " + title.toUpperCase(Locale.ROOT) + "  ")
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.items[?(@.id == " + id + ")]")
                .exists()
                .jsonPath("$.totalItems")
                .isEqualTo(1);
    }

    // --- update ---

    @Test
    void updateReplacesGenresAndKeepsPoster() {
        long id = createMovie("Before update", genreIds(2));
        client.put()
                .uri("/api/admin/movies/{id}/poster", id)
                .headers(h -> h.setBearerAuth(adminToken()))
                .body(BodyInserters.fromMultipartData(posterUpload(MediaType.IMAGE_JPEG)))
                .exchange()
                .expectStatus()
                .isOk();

        client.put()
                .uri("/api/admin/movies/{id}", id)
                .headers(h -> h.setBearerAuth(adminToken()))
                .bodyValue(Map.of("title", "After update", "durationMin", 99, "genreIds", genreIds(1)))
                .exchange()
                .expectStatus()
                .isOk();

        client.get()
                .uri("/api/movies/{id}", id)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("After update")
                .jsonPath("$.durationMin")
                .isEqualTo(99)
                .jsonPath("$.genres.length()")
                .isEqualTo(1)
                .jsonPath("$.posterUrl")
                .exists();
    }

    @Test
    void deleteRemovesMovie() {
        long id = createMovie("To be deleted", genreIds(1));

        client.delete()
                .uri("/api/admin/movies/{id}", id)
                .headers(h -> h.setBearerAuth(adminToken()))
                .exchange()
                .expectStatus()
                .isNoContent();

        client.get().uri("/api/movies/{id}", id).exchange().expectStatus().isNotFound();
    }

    // --- poster ---

    @Test
    void uploadedPosterIsServed() {
        long id = createMovie("Poster test", genreIds(1));

        Map<?, ?> movie = client.put()
                .uri("/api/admin/movies/{id}/poster", id)
                .headers(h -> h.setBearerAuth(adminToken()))
                .body(BodyInserters.fromMultipartData(posterUpload(MediaType.IMAGE_JPEG)))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        String posterUrl = String.valueOf(Objects.requireNonNull(movie).get("posterUrl"));

        client.get().uri(posterUrl).exchange().expectStatus().isOk();
    }

    // --- helpers ---

    /** A multipart body with a tiny fake file declared as the given type. */
    private static MultiValueMap<String, HttpEntity<?>> posterUpload(MediaType type) {
        MultipartBodyBuilder multipart = new MultipartBodyBuilder();
        multipart
                .part("file", new ByteArrayResource(new byte[] {1, 2, 3}) {
                    @Override
                    public String getFilename() {
                        return "poster.bin";
                    }
                })
                .contentType(type);
        return multipart.build();
    }
}
