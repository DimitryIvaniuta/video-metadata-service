package com.github.dimitryivaniuta.videometadata.web.dto.imports;

import com.github.dimitryivaniuta.videometadata.model.Video;
import com.github.dimitryivaniuta.videometadata.model.VideoCategory;
import com.github.dimitryivaniuta.videometadata.model.VideoProvider;
import com.github.dimitryivaniuta.videometadata.util.DateTimeUtil;
import lombok.Builder;

import java.time.Instant;
import java.time.OffsetDateTime;

@Builder
public record VideoResponse(
        Long id,
        String title,
        String source,
        Long durationMs,
        String description,
        VideoCategory videoCategory,
        VideoProvider videoProvider,
        String externalVideoId,
        OffsetDateTime uploadDate,
        Long createdUserId
) {
    public static VideoResponse toDto(Video v) {
        return VideoResponse.builder()
                .id(v.getId())
                .title(v.getTitle())
                .source(v.getSource())
                .durationMs(v.getDurationMs())
                .description(v.getDescription())
                .videoCategory(v.getCategory())
                .videoProvider(v.getProvider())
                .externalVideoId(v.getExternalVideoId())
                .uploadDate(DateTimeUtil.toOffset(v.getUploadDate()))
                .createdUserId(v.getCreatedUserId())
                .build();
    }
}