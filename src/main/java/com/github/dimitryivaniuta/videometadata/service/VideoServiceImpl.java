package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.config.VideoProvidersProperties;
import com.github.dimitryivaniuta.videometadata.domain.command.ImportVideoCommand;
import com.github.dimitryivaniuta.videometadata.model.Video;
import com.github.dimitryivaniuta.videometadata.model.VideoProvider;
import com.github.dimitryivaniuta.videometadata.repository.VideoRepository;
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
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;

import java.time.Instant;

/**
 * Reactive application service that:
 * <ol>
 *   <li>Checks for duplicates in the read model.</li>
 *   <li>Fetches external metadata (YouTube / Vimeo) through {@link ExternalMetadataClient}.</li>
 *   <li>Dispatches an {@link ImportVideoCommand} (no ID pre‑allocation; DB assigns it).</li>
 *   <li>Returns a {@link VideoResponse} containing the generated ID.</li>
 *   <li>Is fully protected by Resilience4j (CB, Retry, RL, Bulkhead).</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoServiceImpl implements VideoService {

    private static final String RESILIENT_NAME = "videoImport";

    private final CommandGateway gateway;
    private final ExternalMetadataClient meta;
    private final VideoRepository videoRepo;
    private final VideoProvidersProperties props;
    private final UserCacheService userCache;

    @Override
    public Flux<VideoResponse> list(int page, int size) {
        return videoRepo.findAll()
                .skip((long) page * size)
                .take(size)
                .map(this::toDto);
    }

    @Override
    public Mono<VideoResponse> getById(Long id) {
        return videoRepo.findById(id).map(this::toDto);
    }

    @CircuitBreaker(name = RESILIENT_NAME, fallbackMethod = "importFallback")
    @Retry(name = RESILIENT_NAME)
    @RateLimiter(name = RESILIENT_NAME)
    @Bulkhead(name = RESILIENT_NAME, type = Bulkhead.Type.SEMAPHORE)
    @Override
    public Mono<VideoResponse> importVideo(VideoProvider providerKey, String externalVideoId) {

        if (!props.getProviders().containsKey(providerKey.name().toLowerCase())) {
            return Mono.error(new IllegalArgumentException("Unknown provider: " + providerKey));
        }

        // 1) fast‑path: already imported?
        Mono<VideoResponse> duplicate = videoRepo
                .findByProviderAndExternalVideoId(providerKey, externalVideoId)
                .map(this::toDto);

        // 2) otherwise fetch metadata + dispatch command
        return duplicate.switchIfEmpty(
                resolveUserId()
                        .flatMap(userId ->
                                meta.fetch(providerKey, externalVideoId)
                                        .flatMap(md -> sendCommand(userId, md)))
        );
    }

    /**
     * Retrieve numeric userId from cache (reactive SecurityContext → username → cache).
     */
    private Mono<Long> resolveUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(userCache::getUser)
                .map(CachedUser::id)
                .doOnNext(id -> log.debug("Import requested by userId={}", id));
    }

    /**
     * Send command and map command result (generated ID) to DTO.
     */
    private Mono<VideoResponse> sendCommand(Long userId, Metadata md) {
        ImportVideoCommand cmd = ImportVideoCommand.builder()
                .externalVideoId(md.externalVideoId())
                .title(md.title())
                .description(md.description())
                .durationMs(md.durationMs())
                .videoProvider(md.videoProvider())
                .videoCategory(md.videoCategory())
                .uploadDate(md.uploadDate())
                .createdUserId(userId)
                .build();
        return Mono.fromFuture(gateway.send(cmd))      // returns CompletableFuture<Long>
                .cast(Long.class)
                .map(id -> VideoResponse.builder()
                        .id(id)
                        .title(md.title())
                        .description(md.description())
                        .durationMs(md.durationMs())
                        .videoCategory(md.videoCategory())
                        .videoProvider(md.videoProvider())
                        .uploadDate(md.uploadDate())
                        .externalVideoId(md.externalVideoId())
                        .build())
                // DuplicateKeyException may occur in rare race; fall back to existing row
                .onErrorResume(DuplicateKeyException.class, ex ->
                        videoRepo.findByProviderAndExternalVideoId(md.videoProvider(), md.externalVideoId())
                                .map(this::toDto));
    }

    /**
     * Fallback for resilience annotations.
     */
    @SuppressWarnings("unused")
    private Mono<VideoResponse> importFallback(VideoProvider provider, String externalId, Throwable t) {
        log.warn("Fallback triggered for provider={} id={}, cause={}",
                provider, externalId, t.toString());
        return Mono.error(new IllegalStateException(
                "Temporary failure importing %s/%s".formatted(provider, externalId), t));
    }

    private VideoResponse toDto(Video v) {
        return VideoResponse.builder()
                .id(v.getId())
                .title(v.getTitle())
                .source(v.getSource())
                .durationMs(v.getDurationMs())
                .description(v.getDescription())
                .videoCategory(v.getCategory())
                .videoProvider(v.getProvider())
                .externalVideoId(v.getExternalVideoId())
                .uploadDate(v.getUploadDate())
                .createdUserId(v.getCreatedUserId())
                .build();
    }
}

