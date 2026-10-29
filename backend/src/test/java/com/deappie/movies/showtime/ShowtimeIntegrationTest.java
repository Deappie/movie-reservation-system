package com.deappie.movies.showtime;

import com.deappie.movies.IntegrationTest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShowtimeIntegrationTest extends IntegrationTest {

    // --- halls ---

    @Test
    void adminListsHallsWithCapacity() {
        client.get()
                .uri("/api/admin/halls")
                .headers(h -> h.setBearerAuth(adminToken()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.length()")
                .isEqualTo(2)
                .jsonPath("$[0].name")
                .isEqualTo("Hall 1")
                .jsonPath("$[0].capacity")
                .isEqualTo(96);
    }

    // --- create ---

    @Test
    void createComputesEndFromDurationAndCleaningBuffer() {
        long movieId = createMovie("Endtime " + UUID.randomUUID(), 120, genreIds(1));
        OffsetDateTime startsAt = at(uniqueDay(), 18, 0);
        // 120 min movie + 15 min cleaning buffer
        String expectedEnd = startsAt.toInstant().plus(Duration.ofMinutes(135)).toString();

        client.post()
                .uri("/api/admin/showtimes")
                .headers(h -> h.setBearerAuth(adminToken()))
                .bodyValue(showtimeBody(movieId, hallIds().getFirst(), startsAt))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.startsAt")
                .isEqualTo(startsAt.toInstant().toString())
                .jsonPath("$.endsAt")
                .isEqualTo(expectedEnd);
    }

    @Test
    void overlappingShowtimeInSameHallIsRejected() {
        long movieId = createMovie("Overlap " + UUID.randomUUID(), 120, genreIds(1));
        List<Long> halls = hallIds();
        LocalDate day = uniqueDay();
        // occupies 18:00–20:15 (120 min movie + 15 min cleaning buffer)
        createShowtime(movieId, halls.get(0), at(day, 18, 0));

        // 19:00 in the same hall overlaps
        client.post()
                .uri("/api/admin/showtimes")
                .headers(h -> h.setBearerAuth(adminToken()))
                .bodyValue(showtimeBody(movieId, halls.get(0), at(day, 19, 0)))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Hall is busy at that time");

        // starting exactly when the first one ends is allowed, and so is the same time in another hall
        createShowtime(movieId, halls.get(0), at(day, 20, 15));
        createShowtime(movieId, halls.get(1), at(day, 19, 0));
    }

    @Test
    void unknownHallIsRejected() {
        long movieId = createMovie("Unknown hall " + UUID.randomUUID(), genreIds(1));

        client.post()
                .uri("/api/admin/showtimes")
                .headers(h -> h.setBearerAuth(adminToken()))
                .bodyValue(showtimeBody(movieId, 999_999L, at(uniqueDay(), 18, 0)))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Unknown hall 999999");
    }

    @Test
    void unknownMovieIsRejected() {
        client.post()
                .uri("/api/admin/showtimes")
                .headers(h -> h.setBearerAuth(adminToken()))
                .bodyValue(showtimeBody(999_999L, hallIds().getFirst(), at(uniqueDay(), 18, 0)))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Unknown movie 999999");
    }

    @Test
    void startInThePastIsRejected() {
        long movieId = createMovie("Past " + UUID.randomUUID(), genreIds(1));

        client.post()
                .uri("/api/admin/showtimes")
                .headers(h -> h.setBearerAuth(adminToken()))
                .bodyValue(showtimeBody(movieId, hallIds().getFirst(), at(LocalDate.of(2020, 1, 1), 18, 0)))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.errors.startsAt")
                .exists();
    }

    @Test
    void priceWithMoreThanTwoDecimalsIsRejected() {
        long movieId = createMovie("Price " + UUID.randomUUID(), genreIds(1));

        client.post()
                .uri("/api/admin/showtimes")
                .headers(h -> h.setBearerAuth(adminToken()))
                .bodyValue(Map.of(
                        "movieId",
                        movieId,
                        "hallId",
                        hallIds().getFirst(),
                        "startsAt",
                        iso(at(uniqueDay(), 18, 0)),
                        "basePrice",
                        9.999))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.errors.basePrice")
                .exists();
    }

    @Test
    void userCannotScheduleShowtimes() {
        String token = userToken();

        client.post()
                .uri("/api/admin/showtimes")
                .headers(h -> h.setBearerAuth(token))
                .bodyValue(showtimeBody(1L, 1L, at(uniqueDay(), 18, 0)))
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    // --- list by date ---

    @Test
    void listByDateGroupsShowtimesPerMovieInStartOrder() {
        long movieId = createMovie("Grouped " + UUID.randomUUID(), genreIds(1));
        List<Long> halls = hallIds();
        LocalDate day = uniqueDay();
        createShowtime(movieId, halls.get(1), at(day, 20, 0));
        createShowtime(movieId, halls.get(0), at(day, 14, 0));
        String movie = "$[?(@.movieId == " + movieId + ")]";

        client.get()
                .uri("/api/showtimes?date={date}", day)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath(movie + ".showtimes.length()")
                .isEqualTo(2)
                .jsonPath(movie + ".showtimes[0].startsAt")
                .isEqualTo(at(day, 14, 0).toInstant().toString())
                .jsonPath(movie + ".showtimes[0].seatsLeft")
                .isEqualTo(96);
    }

    @Test
    void showtimeAfterMidnightInRigaBelongsToTheNextDay() {
        long movieId = createMovie("Midnight " + UUID.randomUUID(), genreIds(1));
        LocalDate day = uniqueDay();
        LocalDate nextDay = day.plusDays(1);
        // 00:30 in Riga on nextDay is still `day` in UTC
        createShowtime(movieId, hallIds().getFirst(), at(nextDay, 0, 30));
        String movie = "$[?(@.movieId == " + movieId + ")]";

        client.get()
                .uri("/api/showtimes?date={date}", day)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath(movie)
                .doesNotExist();

        client.get()
                .uri("/api/showtimes?date={date}", nextDay)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath(movie + ".showtimes.length()")
                .isEqualTo(1);
    }

    @Test
    void invalidDateIsRejected() {
        client.get()
                .uri("/api/showtimes?date=05.11.2026")
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    // --- delete ---

    @Test
    void deleteRemovesShowtime() {
        long movieId = createMovie("Delete showtime " + UUID.randomUUID(), genreIds(1));
        LocalDate day = uniqueDay();
        long showtimeId = createShowtime(movieId, hallIds().getFirst(), at(day, 18, 0));

        client.delete()
                .uri("/api/admin/showtimes/{id}", showtimeId)
                .headers(h -> h.setBearerAuth(adminToken()))
                .exchange()
                .expectStatus()
                .isNoContent();

        client.get()
                .uri("/api/showtimes?date={date}", day)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[?(@.movieId == " + movieId + ")]")
                .doesNotExist();

        client.delete()
                .uri("/api/admin/showtimes/{id}", showtimeId)
                .headers(h -> h.setBearerAuth(adminToken()))
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void movieWithShowtimesCannotBeDeleted() {
        long movieId = createMovie("Has showtimes " + UUID.randomUUID(), genreIds(1));
        createShowtime(movieId, hallIds().getFirst(), at(uniqueDay(), 18, 0));

        client.delete()
                .uri("/api/admin/movies/{id}", movieId)
                .headers(h -> h.setBearerAuth(adminToken()))
                .exchange()
                .expectStatus()
                .isEqualTo(409);
    }

    // --- helpers ---

    private static Map<String, Object> showtimeBody(long movieId, long hallId, OffsetDateTime startsAt) {
        return Map.of("movieId", movieId, "hallId", hallId, "startsAt", iso(startsAt), "basePrice", 9.50);
    }
}
