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
    public Flux<Metadata> fetchByPublisher(String handle) {
        return resolveChannelId(handle)                      // step 1
                .flatMapMany(this::fetchAllVideoIdsOfChannel)    // step 2
                .flatMap(this::fetch)                            // step 3
                .onErrorContinue((e, vid) ->
                        log.warn("Skipping video {} due to {}", vid, e.toString()));
    }

    @SuppressWarnings("unused")
    private Flux<Metadata> fallbackBulk(String publisherName, Throwable ex) {
        log.warn("YouTube bulk-fetch fallback for publisher={}: {}", publisherName, ex.toString());
        return Flux.error(new IllegalStateException("YouTube bulk import failed", ex));
    }

    private Mono<String> resolveChannelId(String handle) {
        return wc.get()
                .uri(uri -> uri.path("/search")
                        .queryParam("part", "id")
                        .queryParam("type", "channel")
                        .queryParam("q", handle.startsWith("@") ? handle.substring(1) : handle)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(SearchChannelResponse.class)
                .flatMap(resp -> {
                    if (resp.items() == null || resp.items().isEmpty()) {
                        return Mono.error(new IllegalArgumentException(
                                "No channel found for handle " + handle));
                    }
                    return Mono.just(resp.items().getFirst().id().channelId());
                });
    }

    /* ───────────── helper – page Search API for all videos ──── */

    private Flux<String> fetchAllVideoIdsOfChannel(String channelId) {
        return fetchVideoPage(channelId, null)
                .expand(raw -> raw.nextPageToken() == null
                        ? Mono.empty()
                        : fetchVideoPage(channelId, raw.nextPageToken()))
                .flatMapIterable(raw -> raw.items().stream()
                        .map(item -> item.id().videoId())
                        .toList());
    }

    private Mono<SearchVideoRaw> fetchVideoPage(String channelId, String pageToken) {
        return wc.get()
                .uri(uri -> uri.path("/search")
                        .queryParam("part", "id")
                        .queryParam("channelId", channelId)
                        .queryParam("type", "video")
                        .queryParam("order", "date")
                        .queryParam("maxResults", "50")
                        .queryParam("key", apiKey)
                        .queryParamIfPresent("pageToken",
                                pageToken == null
                                        ? java.util.Optional.empty()
                                        : java.util.Optional.of(pageToken))
                        .build())
                .retrieve()
                .bodyToMono(SearchVideoRaw.class);
    }


}
