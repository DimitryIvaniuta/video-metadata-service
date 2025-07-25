package com.github.dimitryivaniuta.videometadata.web.dto.imports;

import java.time.Instant;

public record VideoResponse(
        Long      id,
        String    title,
        String    source,
        Long      durationMs,
        String    description,
        Short     category,
        Short     provider,
        String    externalVideoId,
        Instant   uploadDate,
        Long      createdUserId
) {}