package com.deappie.movies.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
class LocalPricingService implements PricingService {

    @Override
    public Mono<PriceQuote> quote(PriceRequest request) {
        BigDecimal perSeat = request.basePrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = perSeat.multiply(BigDecimal.valueOf(request.seats()));
        return Mono.just(new PriceQuote(perSeat, total));
    }
}
