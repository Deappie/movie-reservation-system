package com.deappie.movies.pricing;

import reactor.core.publisher.Mono;

public interface PricingService {
    Mono<PriceQuote> quote(PriceRequest request);
}
