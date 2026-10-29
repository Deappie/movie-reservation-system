package com.deappie.movies.hall;

public record HallResponse(long id, String name, int rows, int seatsPerRow, int capacity) {
    static HallResponse from(Hall hall) {
        return new HallResponse(hall.requiredId(), hall.name(), hall.rows(), hall.seatsPerRow(), hall.capacity());
    }
}
