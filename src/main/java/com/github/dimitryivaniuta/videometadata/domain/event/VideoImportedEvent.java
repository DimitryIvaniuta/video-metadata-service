package com.github.dimitryivaniuta.videometadata.domain.event;

import com.github.dimitryivaniuta.videometadata.model.Video;
import com.github.dimitryivaniuta.videometadata.model.VideoCategory;
import com.github.dimitryivaniuta.videometadata.model.VideoProvider;
import lombok.Builder;

import java.time.Instant;

/**
 * Published once a new video import has been persisted.
 * <p>
 * Contains all the information needed to update read‑model projections,
 * perform notifications, analytics, etc.
 *
 * @param id              the database‑generated video ID
 * @param externalVideoId the provider’s video identifier
 * @param title           the video title
 * @param description     the video description
 * @param durationMs      the video duration in milliseconds
 * @param category        the video’s category
 * @param provider        which provider the video came from
 * @param uploadDate      original upload timestamp on the provider
 * @param createdUserId   ID of the user who triggered the import
 */
@Builder
public record VideoImportedEvent(
        Long id,
        String externalVideoId,
        String title,
        String description,
        Long durationMs,
        VideoCategory category,
        VideoProvider provider,
        Instant uploadDate,
        Long createdUserId
) {
}