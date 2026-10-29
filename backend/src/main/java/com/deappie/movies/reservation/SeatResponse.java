package com.deappie.movies.reservation;

public record SeatResponse(long id, String row, int number, boolean taken) {}
