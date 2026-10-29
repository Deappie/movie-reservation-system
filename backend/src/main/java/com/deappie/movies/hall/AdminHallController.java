package com.deappie.movies.hall;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/admin/halls")
@RequiredArgsConstructor
class AdminHallController {
    private final HallService hallService;

    @GetMapping
    Flux<HallResponse> findAll() {
        return hallService.findAll();
    }
}
