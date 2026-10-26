package com.deappie.movies.movie;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
class PosterResourceConfig implements WebFluxConfigurer {
    private final PosterProperties posterProperties;

    PosterResourceConfig(PosterProperties posterProperties) {
        this.posterProperties = posterProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(posterProperties.dir())
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        registry.addResourceHandler("/posters/**").addResourceLocations(location);
    }
}
