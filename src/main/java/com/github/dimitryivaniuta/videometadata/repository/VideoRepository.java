package com.github.dimitryivaniuta.videometadata.repository;

import com.github.dimitryivaniuta.videometadata.model.Video;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;


import com.github.dimitryivaniuta.videometadata.model.Video;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface VideoRepository extends ReactiveCrudRepository<Video, Long> {
    /**
     * Look up by source (e.g. "youtube") and external ID (e.g. "Ks-_Mh1QhMc").
     */
    Mono<Video> findBySourceAndExternalVideoId(String source, String externalVideoId);
}