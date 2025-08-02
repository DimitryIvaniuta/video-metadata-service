package com.github.dimitryivaniuta.videometadata.service.videoprovider;

import com.github.dimitryivaniuta.videometadata.config.VideoProvidersProperties;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.*;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * YouTube ProviderAdapter: single-video + bulk-by-publisher.
 */
@Slf4j
public class YoutubeAdapter implements ProviderAdapter {

    private static final String YT_NAME = "videoMeta-youtube";

    private final WebClient wc;
    private final String apiKey;

    public YoutubeAdapter(VideoProvidersProperties.Provider cfg) {
        this.apiKey = cfg.getApiKey();
        this.wc     = WebClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .build();
    }

    @Override
    @CircuitBreaker(name = YT_NAME, fallbackMethod = "fallbackSingle")
    @Retry(name = YT_NAME)
    @RateLimiter(name = YT_NAME)
    @Bulkhead(name = YT_NAME, type = Bulkhead.Type.SEMAPHORE)
    public Mono<Metadata> fetch(String id) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .defaultIfEmpty("anonymous")
                .flatMap(user ->
                        wc.get()
                                .uri(uri -> uri.path("/videos")
                                        .queryParam("part", "snippet,contentDetails")
                                        .queryParam("id", id)
                                        .queryParam("key", apiKey)
                                        .build())
                                .retrieve()
                                .bodyToMono(ExternalYoutubeResponse.class)
                                .map(resp -> resp.toMetadata(id, user))
                );
    }

    @SuppressWarnings("unused")
    private Mono<Metadata> fallbackSingle(String id, Throwable ex) {
        log.warn("YouTube single-video fallback for id={}: {}", id, ex.toString());
        return Mono.error(new IllegalStateException("YouTube metadata unavailable", ex));
    }

    @Override
    @CircuitBreaker(name = YT_NAME, fallbackMethod = "fallbackBulk")
    @Retry(name = YT_NAME)
    @RateLimiter(name = YT_NAME)
    @Bulkhead(name = YT_NAME, type = Bulkhead.Type.SEMAPHORE)
    public Flux<Metadata> fetchByPublisher(String publisherHandle) {
        // 1) Resolve current user
        Mono<String> userMono = ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .defaultIfEmpty("anonymous");

        // 2) Search channel by handle (uses the Search API)
        Mono<String> channelIdMono = wc.get()
                .uri(uri -> uri.path("/search")
                        .queryParam("part", "snippet")
                        .queryParam("type", "channel")
                        .queryParam("q", publisherHandle)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(SearchChannelResponse.class)
                .flatMap(resp -> {
                    if (resp.items() == null || resp.items().isEmpty()) {
                        return Mono.error(new IllegalArgumentException(
                                "No channel found for handle " + publisherHandle));
                    }
                    return Mono.just(resp.items().getFirst().id().channelId());
                });

        // 3) Fetch uploads playlist ID via Channels API
        Mono<String> uploadsMono = channelIdMono.flatMap(chId ->
                wc.get()
                        .uri(uri -> uri.path("/channels")
                                .queryParam("part", "contentDetails")
                                .queryParam("id", chId)
                                .queryParam("key", apiKey)
                                .build())
                        .retrieve()
                        .bodyToMono(ChannelListResponse.class)
                        .flatMap(resp -> {
                            if (resp.items() == null || resp.items().isEmpty()) {
                                return Mono.error(new IllegalStateException(
                                        "Channel found but no contentDetails for " + chId));
                            }
                            return Mono.just(resp.items().getFirst()
                                    .contentDetails()
                                    .relatedPlaylists()
                                    .get("uploads")
                            );
                        })
        );

        // 4) Page through playlistItems to emit every videoId,
        //    then delegate to fetch(...) for full metadata
        return userMono.flatMapMany(user ->
                uploadsMono.flatMapMany(playlistId ->
                        fetchPlaylistIds(playlistId, null)
                                .flatMap(videoId ->
                                        fetch(videoId)
                                                .onErrorResume(e -> {
                                                    log.warn("Skipping {} due to {}", videoId, e.toString());
                                                    return Mono.empty();
                                                })
                                )
                )
        );
    }

    @SuppressWarnings("unused")
    private Flux<Metadata> fallbackBulk(String publisherName, Throwable ex) {
        log.warn("YouTube bulk-fetch fallback for publisher={}: {}", publisherName, ex.toString());
        return Flux.error(new IllegalStateException("YouTube bulk import failed", ex));
    }

    /** Recursively page through /playlistItems to collect every videoId. */
    private Flux<String> fetchPlaylistIds(String playlistId, String pageToken) {
        return wc.get()
                .uri(uri -> uri.path("/playlistItems")
                        .queryParam("part", "contentDetails")
                        .queryParam("playlistId", playlistId)
                        .queryParam("maxResults", "50")
                        .queryParam("key", apiKey)
                        .queryParamIfPresent("pageToken",
                                pageToken == null
                                        ? java.util.Optional.empty()
                                        : java.util.Optional.of(pageToken))
                        .build())
                .retrieve()
                .bodyToMono(PlaylistResponse.class)
                .flatMapMany(resp -> {
                    Flux<String> ids = Flux.fromIterable(resp.items())
                            .map(item -> item.contentDetails().videoId());
                    if (resp.nextPageToken() != null) {
                        return ids.concatWith(fetchPlaylistIds(playlistId, resp.nextPageToken()));
                    }
                    return ids;
                });
    }

}
