// src/main/java/com/github/dimitryivaniuta/videometadata/service/videoprovider/VimeoAdapter.java
package com.github.dimitryivaniuta.videometadata.service.videoprovider;

import com.github.dimitryivaniuta.videometadata.config.VideoProvidersProperties;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.ExternalVimeoResponse;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.Metadata;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Vimeo ProviderAdapter: only single‐video is supported.
 */
@Slf4j
public class VimeoAdapter implements ProviderAdapter {

    private static final String NAME = "videoMeta-vimeo";

    private final WebClient wc;

    public VimeoAdapter(VideoProvidersProperties.Provider cfg) {
        // attach Bearer token to every request
        this.wc = WebClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + cfg.getAccessToken())
                .build();
    }

    @Override
    @CircuitBreaker(name = NAME, fallbackMethod = "fallbackSingle")
    @Retry(name = NAME)
    @RateLimiter(name = NAME)
    @Bulkhead(name = NAME, type = Bulkhead.Type.SEMAPHORE)
    public Mono<Metadata> fetch(String id) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .defaultIfEmpty("anonymous")
                .flatMap(user ->
                        wc.get()
                                .uri("/videos/{id}", id)
                                .retrieve()
                                .bodyToMono(ExternalVimeoResponse.class)
                                .map(resp -> resp.toMetadata(id, user))
                );
    }

    @SuppressWarnings("unused")
    private Mono<Metadata> fallbackSingle(String id, Throwable ex) {
        log.warn("Vimeo single-video fallback for id={}: {}", id, ex.toString());
        return Mono.error(new IllegalStateException("Vimeo metadata unavailable", ex));
    }

}
