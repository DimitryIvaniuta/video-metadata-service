package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.web.dto.VideoConnection;
import com.github.dimitryivaniuta.videometadata.web.dto.graphql.types.VideoSort;
import reactor.core.publisher.Mono;

public interface VideoQueryService {

    /**
     * Fetch a page of videos with optional provider filter and sorting.
     *
     * @param page      1-based page index (defaults to 1)
     * @param pageSize  items per page (defaults to 20, max 100)
     * @param provider  optional provider filter (matches {@code videos.source})
     * @param sortBy    sort field (defaults to IMPORTED_AT)
     * @param sortDesc  sort direction (defaults to true/desc)
     */
    Mono<VideoConnection> fetchVideos(
            Integer page,
            Integer pageSize,
            String provider,
            VideoSort sortBy,
            Boolean sortDesc
    );

    /**
     * Count videos, optionally filtered by provider.
     */
    Mono<Long> countVideos(String provider);
}
