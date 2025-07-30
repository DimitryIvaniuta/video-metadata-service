package com.github.dimitryivaniuta.videometadata.service.videoprovider;

import com.github.dimitryivaniuta.videometadata.web.dto.imports.ExternalVimeoResponse;
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
 * Vimeo API adapter.
 */
@Slf4j
public class VimeoAdapter implements ProviderAdapter {

    private static final String VM_NAME = "videoMeta-vimeo";

    private final WebClient wc;

    VimeoAdapter(WebClient wc, String accessToken) {
        this.wc = wc.mutate()
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();
    }

    @Override
    @CircuitBreaker(name = VM_NAME, fallbackMethod = "fallback")
    @RateLimiter(name = VM_NAME)
    @Retry(name = VM_NAME)
    @Bulkhead(name = VM_NAME, type = Bulkhead.Type.SEMAPHORE)
    public Mono<Metadata> fetch(String id) {

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .ofType(UserDetails.class)
                .map(UserDetails::getUsername)
                .defaultIfEmpty("anonymous")
                .flatMap(reqUser ->
                        wc.get()
                                .uri("/videos/{id}", id)
                                .retrieve()
                                .bodyToMono(ExternalVimeoResponse.class)
                                .map(resp -> resp.toMetadata(id, reqUser)));
    }

    @SuppressWarnings("unused")
    private Mono<Metadata> fallback(String id, Throwable ex) {
        log.warn("Vimeo metadata fallback for id={}: {}", id, ex.toString());
        return Mono.error(new IllegalStateException("Vimeo metadata unavailable"));
    }
}