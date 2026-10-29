package com.deappie.movies.pricing;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceRequest(long userId, Instant showtimeStart, BigDecimal basePrice, int seats) {}
