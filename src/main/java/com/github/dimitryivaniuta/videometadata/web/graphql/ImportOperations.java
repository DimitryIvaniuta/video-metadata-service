package com.github.dimitryivaniuta.videometadata.web.graphql;

import com.github.dimitryivaniuta.videometadata.graphql.annotations.*;
import com.github.dimitryivaniuta.videometadata.graphql.schema.RequiresRole;
import com.github.dimitryivaniuta.videometadata.model.VideoProvider;
import com.github.dimitryivaniuta.videometadata.service.VideoService;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.VideoResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

/**
 * GraphQL mutations for importing external video metadata (CQRS command side).
 */
@Component
@GraphQLApplication
@RequiredArgsConstructor
public class ImportOperations {

    private final VideoService videoService;   // internally may dispatch a command/event

    @GraphQLMutation("importVideo")
    @RequiresRole({"USER", "ADMIN"})
    public Mono<VideoResponse> importVideo(
            @GraphQLArgument("provider") @NotNull VideoProvider provider,
            @GraphQLArgument("externalVideoId") @NotBlank String externalVideoId) {

        // The service encapsulates CQRS: it can publish a command and return a projection.
        return videoService.importVideo(provider, externalVideoId);
    }

    @GraphQLMutation("importVideosByPublisher")
    @RequiresRole({"USER", "ADMIN"})
    public Flux<VideoResponse> importVideosByPublisher(
            @GraphQLArgument("provider") @NotNull VideoProvider provider,
            @GraphQLArgument("publisherName") @NotBlank String publisherName
    ) {
        return videoService.importVideosByPublisher(provider, publisherName);
    }

}
