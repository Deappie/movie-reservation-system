package com.deappie.movies;

import com.deappie.movies.genre.GenreResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@Import(TestcontainersConfiguration.class)
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BackendApplicationTests {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void contextLoads() {}

    @Test
    void genresAreSeeded() {
        webTestClient
                .get()
                .uri("/api/genres")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(GenreResponse.class)
                .hasSize(10);
    }

    @Test
    void unknownGenreReturns404() {
        webTestClient
                .get()
                .uri("/api/genres/999999")
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Genre 999999 not found");
    }
}
