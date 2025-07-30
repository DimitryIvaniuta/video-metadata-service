package com.github.dimitryivaniuta.videometadata.web.dto.imports;

import com.github.dimitryivaniuta.videometadata.model.VideoCategory;
import com.github.dimitryivaniuta.videometadata.model.VideoProvider;
import lombok.Builder;

import java.time.Instant;

@Builder
public record Metadata(
        String title,
        String description,
        Long   durationMs,
        VideoCategory videoCategory,
        VideoProvider videoProvider,
        String externalVideoId,
        Instant uploadDate,
        String requestedBy) {
}