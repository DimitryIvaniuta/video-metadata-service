package com.github.dimitryivaniuta.videometadata.repository;

import com.github.dimitryivaniuta.videometadata.model.Video;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;


import com.github.dimitryivaniuta.videometadata.model.Video;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import com.github.dimitryivaniuta.videometadata.model.Video;
import com.github.dimitryivaniuta.videometadata.model.VideoProvider;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface VideoRepository extends ReactiveCrudRepository<Video, Long> {
    Mono<Video> findByProviderAndExternalVideoId(VideoProvider provider, String externalVideoId);
}