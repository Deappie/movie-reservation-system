package com.deappie.movies;

import com.deappie.movies.genre.GenreResponse;
import org.junit.jupiter.api.Test;

class BackendApplicationTests extends IntegrationTest {

    @Test
    void contextLoads() {}

    @Test
    void genresAreSeeded() {
        client.get()
                .uri("/api/genres")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(GenreResponse.class)
                .hasSize(10);
    }

    @Test
    void unknownGenreReturns404() {
        client.get()
                .uri("/api/genres/999999")
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Genre 999999 not found");
    }
}
