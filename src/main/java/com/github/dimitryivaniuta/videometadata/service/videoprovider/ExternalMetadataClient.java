package com.github.dimitryivaniuta.videometadata.service.videoprovider;

import com.github.dimitryivaniuta.videometadata.config.VideoProvidersProperties;
import com.github.dimitryivaniuta.videometadata.model.VideoProvider;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.ExternalVimeoResponse;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.Metadata;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Facade for fetching external video metadata (YouTube, Vimeo, …).
 * <p>
 * Call {@link #fetch(VideoProvider, String)} with provider key and external video id.
 * Provider keys must match those defined in {@code video-providers.yml}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalMetadataClient {

    private final VideoProvidersProperties props;
    private final WebClient.Builder        http;

    // Registry of ProviderAdapters (lazy‑built, thread‑safe)
    private final Map<VideoProvider, ProviderAdapter> adapterCache = new ConcurrentHashMap<>();

    public Mono<Metadata> fetch(VideoProvider providerKey, String externalVideoId) {
        ProviderAdapter adapter = adapterCache.computeIfAbsent(
                providerKey,
                this::createAdapter);

        if (adapter == null) {
            return Mono.error(new IllegalArgumentException("Unsupported provider: " + providerKey));
        }
        return adapter.fetch(externalVideoId);
    }

    // ProviderAdapter factory
    private ProviderAdapter createAdapter(@NotNull VideoProvider key) {
        VideoProvidersProperties.Provider cfg = props.getProviders().get(key.name().toLowerCase());
        if (cfg == null) return null;

        WebClient base = http
                .baseUrl(cfg.getBaseUrl())
                .defaultHeaders(h -> h.setAccept(MediaType.parseMediaTypes("application/json")))
                .build();

        return switch (key) {
            case VideoProvider.YOUTUBE -> new YoutubeAdapter(base, cfg.getApiKey());
            case VideoProvider.VIMEO   -> new VimeoAdapter(base, cfg.getAccessToken());
            default        -> null;
        };
    }
}
