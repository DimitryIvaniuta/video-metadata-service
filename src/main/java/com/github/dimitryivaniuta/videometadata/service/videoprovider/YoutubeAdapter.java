package com.github.dimitryivaniuta.videometadata.service.videoprovider;

import com.github.dimitryivaniuta.videometadata.web.dto.imports.ExternalYoutubeResponse;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.Metadata;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * YouTube Data API v3 adapter.
 */
@Slf4j
public class YoutubeAdapter implements ProviderAdapter {

    private static final String YT_NAME = "videoMeta-youtube";

    private final WebClient wc;
    private final String apiKey;

    YoutubeAdapter(WebClient wc, String apiKey) {
        this.wc = wc;
        this.apiKey = apiKey;
    }

    @Override
    @CircuitBreaker(name = YT_NAME, fallbackMethod = "fallback")
    @RateLimiter(name = YT_NAME)
    @Retry(name = YT_NAME)
    @Bulkhead(name = YT_NAME, type = Bulkhead.Type.SEMAPHORE)
    public Mono<Metadata> fetch(String id) {

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .ofType(UserDetails.class)
                .map(u -> u.getUsername())                  // assume username == userId
                .defaultIfEmpty("anonymous")
                .flatMap(reqUser ->
                        wc.get()
                                .uri(uri -> uri.path("/videos")
                                        .queryParam("part", "snippet,contentDetails")
                                        .queryParam("id", id)
                                        .queryParam("key", apiKey)
                                        .build())
                                .retrieve()
                                .bodyToMono(ExternalYoutubeResponse.class)
                                .map(resp -> resp.toMetadata(id, reqUser)));
    }

    /* Resilience4j fallback signature (id + Throwable) */
    @SuppressWarnings("unused")
    private Mono<Metadata> fallback(String id, Throwable ex) {
        log.warn("YouTube metadata fallback for id={}: {}", id, ex.toString());
        return Mono.error(new IllegalStateException("YouTube metadata unavailable"));
    }
}