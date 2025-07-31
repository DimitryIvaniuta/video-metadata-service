package com.github.dimitryivaniuta.videometadata.domain.command;

import com.github.dimitryivaniuta.videometadata.model.Video;
import com.github.dimitryivaniuta.videometadata.model.VideoCategory;
import com.github.dimitryivaniuta.videometadata.model.VideoProvider;
import lombok.Builder;

import java.time.Instant;

/**
 * Command to import a video’s metadata from an external provider.
 * <p>
 * Handled by {@code ImportVideoCommandHandler}, which:
 * <ol>
 *   <li>saves a new VideoWriteEntity (letting the DB auto‑generate the ID),</li>
 *   <li>publishes a {@link com.github.dimitryivaniuta.videometadata.domain.event.VideoImportedEvent},</li>
 *   <li>and returns the generated ID.</li>
 * </ol>
 *
 * @param externalVideoId the provider’s video ID
 * @param title           the video’s title
 * @param description     the video’s description
 * @param durationMs      duration in milliseconds
 * @param videoCategory   numeric category code (provider‑specific)
 * @param videoProvider   the key of the provider (e.g. "youtube", "vimeo")
 * @param uploadDate      the original upload timestamp
 * @param createdUserId   the ID of the user who triggered the import
 */
@Builder
public record ImportVideoCommand(
        String externalVideoId,
        String title,
        String description,
        Long durationMs,
        VideoCategory videoCategory,
        VideoProvider videoProvider,
        Instant uploadDate,
        Long createdUserId
) {

    public Video toVideo() {
        return Video.builder()
                .title(this.title())
                .category(this.videoCategory())
                .provider(this.videoProvider())
                .durationMs(this.durationMs())
                .description(this.description())
                .uploadDate(this.uploadDate())
                .uploadDate(this.uploadDate())
                .uploadDate(this.uploadDate() == null ? Instant.now() : this.uploadDate())
                .build();
    }
}