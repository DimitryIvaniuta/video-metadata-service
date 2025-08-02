package com.github.dimitryivaniuta.videometadata.service;


import com.github.dimitryivaniuta.videometadata.config.VideoProvidersProperties;
import com.github.dimitryivaniuta.videometadata.domain.event.VideoImportedEvent;
import com.github.dimitryivaniuta.videometadata.model.Video;
import com.github.dimitryivaniuta.videometadata.model.VideoProvider;
import com.github.dimitryivaniuta.videometadata.repository.VideoRepository;
import com.github.dimitryivaniuta.videometadata.service.UserCacheService;
import com.github.dimitryivaniuta.videometadata.service.VideoService;
import com.github.dimitryivaniuta.videometadata.service.videoprovider.ExternalMetadataClient;
import com.github.dimitryivaniuta.videometadata.web.dto.CachedUser;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.Metadata;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.VideoResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoServiceImpl implements VideoService {

    private static final String RESILIENT_NAME = "videoImport";

    private final ApplicationEventPublisher     publisher;
    private final ExternalMetadataClient meta;
    private final VideoRepository videoRepo;
    private final VideoProvidersProperties props;
    private final UserCacheService userCache;

    @Override
    public Flux<VideoResponse> list(int page, int size) {
        return videoRepo.findAll()
                .skip((long) page * size)
                .take(size)
                .map(VideoResponse::toDto);
    }

    @Override
    public Mono<VideoResponse> getById(Long id) {
        return videoRepo.findById(id)
                .map(VideoResponse::toDto);
    }

    @CircuitBreaker(name = RESILIENT_NAME, fallbackMethod = "importFallback")
    @Retry(name = RESILIENT_NAME)
    @RateLimiter(name = RESILIENT_NAME)
    @Bulkhead(name = RESILIENT_NAME, type = Bulkhead.Type.SEMAPHORE)
    @Override
    public Mono<VideoResponse> importVideo(VideoProvider provider, String externalVideoId) {
        // Validate provider
        String providerKey = provider.name().toLowerCase();
        if (!props.getProviders().containsKey(providerKey)) {
            return Mono.error(new IllegalArgumentException("Unknown provider: " + providerKey));
        }

        // 1) fast‑path: already imported?
        Mono<VideoResponse> duplicate = videoRepo
                .findByProviderAndExternalVideoId(provider, externalVideoId)
                .map(VideoResponse::toDto);

        // 2) Otherwise fetch, save, publish, return
        Mono<VideoResponse> fresh = resolveUserId()
                .flatMap(userId ->
                        meta.fetch(provider, externalVideoId)
                                .flatMap(md -> saveAndPublish(userId, md))
                );

        // 2) otherwise fetch metadata + dispatch command
        return duplicate.switchIfEmpty(fresh);
    }


    private Mono<Long> resolveUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(userCache::getUser)
                .map(CachedUser::id)
                .doOnNext(id -> log.debug("Import requested by userId={}", id));
    }

    private Mono<VideoResponse> saveAndPublish(Long userId, Metadata md) {
        Video entity = Video.builder()
                .title(md.title())
                .source(md.videoProvider().name().toLowerCase())
                .description(md.description())
                .durationMs(md.durationMs())
                .provider(md.videoProvider())
                .category(md.videoCategory())
                .externalVideoId(md.externalVideoId())
                .uploadDate(md.uploadDate())
                .createdUserId(userId)
                .createdAt(Instant.now())
                .build();

        return videoRepo.save(entity)
                .doOnSuccess(saved -> {
                    var evt = VideoImportedEvent.builder()
                            .id(saved.getId())
                            .title(saved.getTitle())
                            .provider(saved.getProvider())
                            .category(saved.getCategory())
                            .externalVideoId(saved.getExternalVideoId())
                            .uploadDate(saved.getUploadDate())
                            .durationMs(saved.getDurationMs())
                            .createdAt(saved.getCreatedAt())
                            .build();
                    publisher.publishEvent(evt);
                    log.debug("Published VideoImportedEvent for id={}", saved.getId());
                })
                .map(VideoResponse::toDto)
                .onErrorResume(DuplicateKeyException.class, ex ->
                        videoRepo.findByProviderAndExternalVideoId(md.videoProvider(), md.externalVideoId())
                                .map(VideoResponse::toDto)
                );
    }



    @SuppressWarnings("unused")
    private Mono<VideoResponse> importFallback(VideoProvider provider, String externalId, Throwable t) {
        log.warn("Fallback triggered for provider={} id={}, cause={}",
                provider, externalId, t.toString());
        return Mono.error(new IllegalStateException(
                "Could not import video %s/%s".formatted(provider, externalId), t));
    }

}

