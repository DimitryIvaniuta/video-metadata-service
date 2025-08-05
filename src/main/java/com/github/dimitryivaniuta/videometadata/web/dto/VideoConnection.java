package com.github.dimitryivaniuta.videometadata.web.dto;

import com.github.dimitryivaniuta.videometadata.web.dto.imports.VideoResponse;
import lombok.Builder;

import java.util.List;

/**
 * Paged result for videos.
 */
@Builder
public record VideoConnection(
        List<VideoResponse> items,
        int page,
        int pageSize,
        long total
) {}