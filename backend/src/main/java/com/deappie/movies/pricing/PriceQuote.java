package com.deappie.movies.pricing;

import java.math.BigDecimal;

public record PriceQuote(BigDecimal pricePerSeat, BigDecimal total) {}
