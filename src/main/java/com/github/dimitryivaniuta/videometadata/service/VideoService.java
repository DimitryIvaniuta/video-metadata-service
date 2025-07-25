package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.web.dto.imports.VideoResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface VideoService {
    Flux<VideoResponse> list(int page, int size);
    Mono<VideoResponse> getById(Long id);

    /**
     * Import metadata for one external video ID from the given provider.
     */
    Mono<VideoResponse> importVideo(String provider, String externalVideoId);
}
