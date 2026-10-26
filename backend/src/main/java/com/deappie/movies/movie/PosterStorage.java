package com.deappie.movies.movie;

import com.deappie.movies.common.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
class PosterStorage {
    private static final Map<MediaType, String> ALLOWED = Map.of(
            MediaType.IMAGE_JPEG, "jpg", MediaType.IMAGE_PNG, "png", MediaType.parseMediaType("image/webp"), "webp");

    private final Path dir;

    PosterStorage(PosterProperties props) throws IOException {
        this.dir = Path.of(props.dir()).toAbsolutePath().normalize();
        Files.createDirectories(dir);
    }

    Mono<String> store(FilePart file) {
        MediaType type = file.headers().getContentType();
        String extension = type == null ? null : ALLOWED.get(type);
        if (extension == null) {
            return Mono.error(() -> new BadRequestException("Poster must be a JPEG, PNG or WebP image"));
        }
        String filename = UUID.randomUUID() + "." + extension;
        return file.transferTo(dir.resolve(filename)).thenReturn("/posters/" + filename);
    }

    Mono<Void> delete(@Nullable String url) {
        if (url == null) {
            return Mono.empty();
        }
        String filename = url.substring(url.lastIndexOf('/') + 1);
        return Mono.fromCallable(() -> Files.deleteIfExists(dir.resolve(filename)))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
