package com.github.dimitryivaniuta.videometadata.handler;

import com.github.dimitryivaniuta.videometadata.domain.command.ImportVideoCommand;
import com.github.dimitryivaniuta.videometadata.domain.event.VideoImportedEvent;
import com.github.dimitryivaniuta.videometadata.model.Video;
import com.github.dimitryivaniuta.videometadata.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventhandling.GenericEventMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import org.axonframework.eventhandling.EventBus;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImportVideoCommandHandler {

    private final VideoRepository repo;
    private final EventBus eventBus;

    @CommandHandler
    public Mono<Long> handle(ImportVideoCommand cmd) {
        // Build write‑model entity; leave ID null for DB to generate
        return repo.save(cmd.toVideo())
                .doOnSuccess(saved -> {
                    // Publish the domain event with all necessary fields
                    VideoImportedEvent evt = VideoImportedEvent.builder()
                            .id(saved.getId())
                            .provider(saved.getProvider())
                            .externalVideoId(saved.getExternalVideoId())
                            .title(saved.getTitle())
                            .description(saved.getDescription())
                            .durationMs(saved.getDurationMs())
                            .category(saved.getCategory())
                            .uploadDate(saved.getUploadDate())
                            .createdUserId(saved.getCreatedUserId())
                            .build();
                    eventBus.publish(GenericEventMessage.asEventMessage(evt));
                    log.debug("Published VideoImportedEvent for id={}", saved.getId());
                })
                .map(Video::getId);
    }
}
