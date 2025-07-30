package com.github.dimitryivaniuta.videometadata.web.controller;

import com.github.dimitryivaniuta.videometadata.model.VideoProvider;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.VideoResponse;
import com.github.dimitryivaniuta.videometadata.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.*;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    /** List all videos, paged. */
    @GetMapping
    public Flux<VideoResponse> list(
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        return videoService.list(page, size);
    }

    /** Get by ID (any authenticated user). */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<VideoResponse>> getById(@PathVariable Long id) {
        return videoService.getById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Import a single video’s metadata.
     * Only users with ROLE_USER or ROLE_ADMIN may import.
     */
    @PostMapping("/import/{provider}/{externalId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Mono<ResponseEntity<VideoResponse>> importVideo(
            @PathVariable VideoProvider provider,
            @PathVariable String externalId) {
        return videoService.importVideo(provider, externalId)
                .map(ResponseEntity::ok)
                .onErrorResume(e ->
                        Mono.just(ResponseEntity
                                .status(HttpStatus.BAD_GATEWAY)
                                .body(null)
                        )
                );
    }
}
