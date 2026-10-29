package com.deappie.movies.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import com.deappie.movies.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;

class ReservationIntegrationTest extends IntegrationTest {

    // --- seat map ---

    @Test
    void newShowtimeHasAllSeatsFree() {
        long showtimeId = newShowtime(0);

        client.get()
                .uri("/api/showtimes/{id}/seats", showtimeId)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.length()")
                .isEqualTo(96)
                .jsonPath("$[0].row")
                .isEqualTo("A")
                .jsonPath("$[0].number")
                .isEqualTo(1)
                .jsonPath("$[?(@.taken == true)]")
                .doesNotExist();
    }

    @Test
    void seatMapOfUnknownShowtimeReturns404() {
        client.get()
                .uri("/api/showtimes/999999/seats")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    // --- book ---

    @Test
    void userBooksSeatsAndTheyBecomeTaken() {
        long showtimeId = newShowtime(0);
        List<Long> seats = seatIds(showtimeId).subList(0, 2);

        book(userToken(), showtimeId, seats)
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("ACTIVE")
                .jsonPath("$.totalPrice")
                .isEqualTo(19.0) // 2 × 9.50
                .jsonPath("$.seatIds.length()")
                .isEqualTo(2);

        client.get()
                .uri("/api/showtimes/{id}/seats", showtimeId)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                // exactly the two booked seats are taken (JSON numbers come back as Integer)
                .jsonPath("$[?(@.taken == true)].id")
                .isEqualTo(List.of(seats.get(0).intValue(), seats.get(1).intValue()));
    }

    @Test
    void bookingRequiresLogin() {
        long showtimeId = newShowtime(0);

        client.post()
                .uri("/api/reservations")
                .bodyValue(Map.of(
                        "showtimeId", showtimeId, "seatIds", seatIds(showtimeId).subList(0, 1)))
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void takenSeatReturns409AndNothingElseIsBooked() {
        long showtimeId = newShowtime(0);
        List<Long> seats = seatIds(showtimeId);
        book(userToken(), showtimeId, List.of(seats.get(0))).expectStatus().isCreated();

        // seat 0 is taken, seat 1 is free — the whole request must fail …
        book(userToken(), showtimeId, List.of(seats.get(0), seats.get(1)))
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Some of the selected seats are already taken");

        // … and the transaction must have rolled back: seat 1 is still free
        client.get()
                .uri("/api/showtimes/{id}/seats", showtimeId)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[?(@.id == " + seats.get(1) + ")].taken")
                .isEqualTo(false);
    }

    @Test
    void seatFromAnotherHallIsRejected() {
        long showtimeId = newShowtime(0);
        long seatInOtherHall = seatIds(newShowtime(1)).getFirst();

        book(userToken(), showtimeId, List.of(seatInOtherHall))
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Some seats do not belong to this showtime's hall");
    }

    @Test
    void emptyOrTooManySeatsAreRejected() {
        long showtimeId = newShowtime(0);
        String token = userToken();

        book(token, showtimeId, List.of()).expectStatus().isBadRequest();
        book(token, showtimeId, seatIds(showtimeId).subList(0, 11))
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void unknownShowtimeIsRejected() {
        book(userToken(), 999_999L, List.of(1L))
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Unknown showtime 999999");
    }

    // --- my reservations ---

    @Test
    void myReservationsListsOnlyMyOwn() {
        long showtimeId = newShowtime(0);
        List<Long> seats = seatIds(showtimeId);
        String me = userToken();
        long mine = reservationId(book(me, showtimeId, seats.subList(0, 2)));
        long someoneElses = reservationId(book(userToken(), showtimeId, seats.subList(2, 3)));

        client.get()
                .uri("/api/reservations/me")
                .headers(h -> h.setBearerAuth(me))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.length()")
                .isEqualTo(1)
                .jsonPath("$[0].id")
                .isEqualTo(mine)
                .jsonPath("$[0].seats")
                .isEqualTo(List.of("A1", "A2"))
                .jsonPath("$[0].cancellable")
                .isEqualTo(true)
                .jsonPath("$[?(@.id == " + someoneElses + ")]")
                .doesNotExist();
    }

    // --- cancel ---

    @Test
    void cancellingFreesTheSeatsForSomeoneElse() {
        long showtimeId = newShowtime(0);
        List<Long> seat = List.of(seatIds(showtimeId).getFirst());
        String me = userToken();
        long reservationId = reservationId(book(me, showtimeId, seat));

        cancel(me, reservationId)
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("CANCELLED");

        // the partial unique index only covers active rows, so the seat can be sold again
        book(userToken(), showtimeId, seat).expectStatus().isCreated();
    }

    @Test
    void cancellingTwiceReturns409() {
        long showtimeId = newShowtime(0);
        String me = userToken();
        long reservationId =
                reservationId(book(me, showtimeId, seatIds(showtimeId).subList(0, 1)));
        cancel(me, reservationId).expectStatus().isOk();

        cancel(me, reservationId)
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Reservation is already cancelled");
    }

    @Test
    void someoneElsesReservationLooksLikeItDoesNotExist() {
        long showtimeId = newShowtime(0);
        long reservationId =
                reservationId(book(userToken(), showtimeId, seatIds(showtimeId).subList(0, 1)));

        cancel(userToken(), reservationId)
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Reservation " + reservationId + " not found");
    }

    // --- concurrency ---

    @Test
    void twentyUsersRacingForTheSameSeatProduceExactlyOneBooking() throws Exception {
        long showtimeId = newShowtime(0);
        List<Long> seat = List.of(seatIds(showtimeId).getFirst());
        int users = 20;
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < users; i++) {
            tokens.add(userToken());
        }

        // all threads wait at the gate, then fire at the same moment
        CountDownLatch gate = new CountDownLatch(1);
        List<Future<Integer>> statuses = new ArrayList<>();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (String token : tokens) {
                statuses.add(pool.submit(() -> {
                    gate.await();
                    return book(token, showtimeId, seat)
                            .returnResult(Void.class)
                            .getStatus()
                            .value();
                }));
            }
            gate.countDown();
        }

        List<Integer> codes = new ArrayList<>();
        for (Future<Integer> status : statuses) {
            codes.add(status.get());
        }
        assertThat(codes).filteredOn(code -> code == 201).hasSize(1);
        assertThat(codes).filteredOn(code -> code == 409).hasSize(users - 1);
    }

    // --- helpers ---

    /** A fresh showtime on its own day in the hall with the given index (0 = Hall 1, 96 seats). */
    private long newShowtime(int hallIndex) {
        long movieId = createMovie("Booking " + UUID.randomUUID(), genreIds(1));
        return createShowtime(movieId, hallIds().get(hallIndex), at(uniqueDay(), 18, 0));
    }

    /** All seat ids of a showtime, in seat-map order (A1, A2, …). */
    private List<Long> seatIds(long showtimeId) {
        List<Map<String, Object>> seats = client.get()
                .uri("/api/showtimes/{id}/seats", showtimeId)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult()
                .getResponseBody();
        return Objects.requireNonNull(seats).stream()
                .map(seat -> ((Number) seat.get("id")).longValue())
                .toList();
    }

    private WebTestClient.ResponseSpec book(String token, long showtimeId, List<Long> seatIds) {
        return client.post()
                .uri("/api/reservations")
                .headers(h -> h.setBearerAuth(token))
                .bodyValue(Map.of("showtimeId", showtimeId, "seatIds", seatIds))
                .exchange();
    }

    private WebTestClient.ResponseSpec cancel(String token, long reservationId) {
        return client.post()
                .uri("/api/reservations/{id}/cancel", reservationId)
                .headers(h -> h.setBearerAuth(token))
                .exchange();
    }

    private static long reservationId(WebTestClient.ResponseSpec booking) {
        Map<?, ?> body = booking.expectStatus()
                .isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        return ((Number) Objects.requireNonNull(body).get("id")).longValue();
    }
}
