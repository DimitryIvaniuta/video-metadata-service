package com.github.dimitryivaniuta.videometadata.service.videoprovider;

import com.github.dimitryivaniuta.videometadata.web.dto.imports.Metadata;
import reactor.core.publisher.Mono;

public interface ProviderAdapter {
    Mono<Metadata> fetch(String externalVideoId);
}