package com.github.dimitryivaniuta.videometadata.projection;

import com.github.dimitryivaniuta.videometadata.domain.event.VideoImportedEvent;
import com.github.dimitryivaniuta.videometadata.model.Video;
import com.github.dimitryivaniuta.videometadata.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Listens for VideoImportedEvent and projects it into the
 * read-model table (videos_read).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoProjectionListener {

    private final VideoRepository readRepo;

    /**
     * Handles a new import event by upserting the read-model entity.
     */
    @EventListener
    public void on(VideoImportedEvent evt) {
        Video entity = Video.builder()
                .id(evt.id())
                .title(evt.title())
                .externalVideoId(evt.externalVideoId())
                .description(evt.description())
                .durationMs(evt.durationMs())
                .category(evt.category())
                .uploadDate(evt.uploadDate())
                .createdUserId(evt.createdUserId())
                .build();

        // Save (insert or update) into videos_read table
        readRepo.save(entity)
                .doOnSuccess(e -> log.debug("Projected VideoReadEntity id={}", e.getId()))
                .doOnError(ex -> log.error("Failed to project video id=" + evt.id(), ex))
                .subscribe();
    }
}
