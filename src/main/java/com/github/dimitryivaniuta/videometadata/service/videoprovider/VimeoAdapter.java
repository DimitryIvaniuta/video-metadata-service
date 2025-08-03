package com.github.dimitryivaniuta.videometadata.service.videoprovider;

import com.github.dimitryivaniuta.videometadata.config.VideoProvidersProperties;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.ExternalVimeoResponse;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.Metadata;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.VimeoVideoPage;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * Vimeo ProviderAdapter: only single‐video is supported.
 */
@Slf4j
public class VimeoAdapter implements ProviderAdapter {

    private static final String NAME = "videoMeta-vimeo";

    private final WebClient wc;

    public VimeoAdapter(VideoProvidersProperties.Provider cfg) {
        this.wc = WebClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + cfg.getAccessToken())
                .defaultHeader("Accept", "application/vnd.vimeo.*+json;version=3.4")
                .build();
    }

    @Override
    @CircuitBreaker(name = NAME, fallbackMethod = "fallbackSingle")
    @Retry(name = NAME)
    @RateLimiter(name = NAME)
    @Bulkhead(name = NAME, type = Bulkhead.Type.SEMAPHORE)
    public Mono<Metadata> fetch(String id) {
        return currentUser().flatMap(user ->
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

    @Override
    @CircuitBreaker(name = NAME, fallbackMethod = "fallbackBulk")
    @Retry(name = NAME)
    @RateLimiter(name = NAME)
    @Bulkhead(name = NAME, type = Bulkhead.Type.SEMAPHORE)
    public Flux<Metadata> fetchByPublisher(String userHandle) {

        return currentUser().flatMapMany(user ->
                fetchPage(userHandle, 1)
                        .expand(p -> p.paging().next() != null
                                ? fetchPageByHref(p.paging().next())
                                : Mono.empty())
                        .flatMapIterable(p -> p.data() == null ? Collections.emptyList() : p.data())
                        .map(resp -> resp.toMetadata(extractVideoId(resp.getUri()), user))
        );
    }

    @SuppressWarnings("unused")
    private Flux<Metadata> fallbackBulk(String userHandle, Throwable ex) {
        log.warn("Vimeo bulk-fetch fallback for user={}: {}", userHandle, ex.toString());
        return Flux.error(new IllegalStateException("Vimeo bulk import failed", ex));
    }

    // ─── HELPERS ────────────────────────────────────────────────

    private Mono<String> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .defaultIfEmpty("anonymous");
    }

    private Mono<VimeoVideoPage> fetchPage(String userHandle, int page) {
        return wc.get()
                .uri(uri -> uri.path("/users/{user}/videos")
                        .queryParam("per_page", "50")
                        .queryParam("page", page)
                        .build(userHandle))
                .retrieve()
                .bodyToMono(VimeoVideoPage.class);
    }

    private Mono<VimeoVideoPage> fetchPageByHref(String href) {
        // href is full URL—WebClient will follow it as-is
        return wc.get()
                .uri(href)
                .retrieve()
                .bodyToMono(VimeoVideoPage.class);
    }

    /** Extract numeric ID from “/videos/{id}”. */
    private static String extractVideoId(String uri) {
        int idx = uri.lastIndexOf('/');
        return idx >= 0 ? uri.substring(idx + 1) : uri;
    }
}
