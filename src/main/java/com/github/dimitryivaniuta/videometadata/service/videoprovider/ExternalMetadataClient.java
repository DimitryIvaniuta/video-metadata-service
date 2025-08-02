package com.github.dimitryivaniuta.videometadata.service.videoprovider;

import com.github.dimitryivaniuta.videometadata.config.VideoProvidersProperties;
import com.github.dimitryivaniuta.videometadata.model.VideoProvider;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.Metadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Locale;
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
    private final WebClient.Builder webClientBuilder;

    /**
     * Cache adapters per provider enum.
     */
    private final ConcurrentHashMap<VideoProvider, ProviderAdapter> adapters = new ConcurrentHashMap<>();

    /**
     * Fetch one video’s metadata.
     */
    public Mono<Metadata> fetch(VideoProvider provider, String externalVideoId) {
        return Mono.defer(() -> getAdapter(provider).fetch(externalVideoId));
    }

    /**
     * Fetch all videos by publisher (e.g. a YouTube channel).
     */
    public Flux<Metadata> fetchByPublisher(VideoProvider provider, String publisherName) {
        return Flux.defer(() -> getAdapter(provider).fetchByPublisher(publisherName));
    }

    /**
     * Lookup or create the adapter.  Never returns null.
     */
    private ProviderAdapter getAdapter(VideoProvider provider) {
        return adapters.computeIfAbsent(provider, this::createAdapter);
    }

    /**
     * Create a new adapter for the given provider.
     * Throws if the provider isn’t configured or supported.
     */
    private ProviderAdapter createAdapter(VideoProvider provider) {
        var cfg = props.getProviders()
                .get(provider.name().toLowerCase(Locale.ROOT));
        if (cfg == null) {
            throw new IllegalArgumentException("No configuration for provider: " + provider);
        }

//        WebClient client = webClientBuilder
//                .baseUrl(cfg.getBaseUrl())
//                .build();

        return switch (provider) {
            case YOUTUBE -> new YoutubeAdapter(cfg);
            case VIMEO -> new VimeoAdapter(cfg);
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };
    }

}
