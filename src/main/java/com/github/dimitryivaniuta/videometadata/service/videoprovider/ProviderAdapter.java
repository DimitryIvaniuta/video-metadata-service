package com.github.dimitryivaniuta.videometadata.service.videoprovider;

import com.github.dimitryivaniuta.videometadata.web.dto.imports.Metadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProviderAdapter {

    /** Fetch one video’s metadata by its external ID. */
    Mono<Metadata> fetch(String externalVideoId);

    /** Fetch all videos for a publisher; default = unsupported. */
    default Flux<Metadata> fetchByPublisher(String publisherName) {
        return Flux.error(new UnsupportedOperationException(
                "Bulk fetch not supported by this provider"));
    }

}