package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.config.VideoProvidersProperties;
import com.github.dimitryivaniuta.videometadata.web.dto.CachedUser;
import com.github.dimitryivaniuta.videometadata.web.dto.UserResponse;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.ExternalVimeoResponse;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.ExternalYoutubeResponse;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.VideoResponse;
import com.github.dimitryivaniuta.videometadata.model.Video;
import com.github.dimitryivaniuta.videometadata.repository.VideoRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.*;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoServiceImpl implements VideoService {

    private final VideoRepository           videoRepo;
    private final VideoProvidersProperties props;
//    private final UserService               userService;
    private final UserCacheService               userCacheService;
    private final WebClient.Builder         webClientBuilder;

    @Override
    public Flux<VideoResponse> list(int page, int size) {
        return videoRepo.findAll()
                .skip((long) page * size)
                .take(size)
                .map(this::toDto);
    }

    @Override
    public Mono<VideoResponse> getById(Long id) {
        return videoRepo.findById(id)
                .map(this::toDto);
    }

    @Override
    @CircuitBreaker(name = "videoImport", fallbackMethod = "importFallback")
    @Retry(name = "videoImport")
    @RateLimiter(name = "videoImport")
    public Mono<VideoResponse> importVideo(String providerKey, String externalVideoId) {
        var cfg = props.getProviders().get(providerKey);
        if (cfg == null) {
            return Mono.error(new IllegalArgumentException("Unknown provider: " + providerKey));
        }
        // 1) Check if we already have this video
        return videoRepo.findBySourceAndExternalVideoId(providerKey, externalVideoId)
                // If found, return it (no external call)
                .flatMap(existing -> Mono.just(toDto(existing)))
                // Otherwise fetch & save
                .switchIfEmpty(fetchAndSave(providerKey, externalVideoId, cfg));
    }


    /**
     * Calls external API, saves the new Video, and returns the DTO.
     */
    private Mono<VideoResponse> fetchAndSave(String providerKey,
                                             String externalVideoId,
                                             VideoProvidersProperties.Provider cfg) {
        // get current user ID
        Mono<Long> userIdMono = ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(userCacheService::getUser)
                .map(CachedUser::id)
                .doOnNext(userId ->
                        log.debug("Importing video on behalf of userId={}", userId)
                );;

        // build WebClient
        WebClient client = webClientBuilder.baseUrl(cfg.getBaseUrl()).build();

        // prepare external fetch
        Mono<ExternalVideo> ext = switch (providerKey) {
            case "youtube" -> client.get()
                    .uri(uri -> uri.path("/videos")
                            .queryParam("part", "snippet,contentDetails")
                            .queryParam("id", externalVideoId)
                            .queryParam("key", cfg.getApiKey())
                            .build())
                    .retrieve().bodyToMono(ExternalYoutubeResponse.class)
                    .flatMap(resp -> Mono.just(parseYoutube(resp)));

            case "vimeo" -> client.get()
                    .uri("/videos/{id}", externalVideoId)
                    .headers(h -> h.setBearerAuth(cfg.getAccessToken()))
                    .retrieve().bodyToMono(ExternalVimeoResponse.class)
                    .flatMap(resp -> Mono.just(parseVimeo(resp)));

            default -> Mono.error(new IllegalStateException("Unsupported provider"));
        };

        // combine userId + external data + save
        return userIdMono.zipWith(ext)
                .flatMap(tuple -> {
                    Long userId = tuple.getT1();
                    ExternalVideo e = tuple.getT2();
                    Video v = Video.builder()
                            .title(e.title())
                            .description(e.description())
                            .durationMs(e.durationMs())
                            .source(providerKey)
                            .provider((short)e.providerCode())
                            .category((short)e.categoryCode())
                            .externalVideoId(externalVideoId)
                            .uploadDate(e.uploadDate())
                            .createdUserId(userId)
                            .build();

                    // Save, let DB unique constraint prevent dups
                    return videoRepo.save(v);
                })
                // map to DTO
                .map(this::toDto)
                // fallback for constraint violation
                .onErrorResume(ex -> {
                    if (ex instanceof org.springframework.dao.DuplicateKeyException ||
                            ex.getMessage().contains("uq_videos_source_external")) {
                        // Race: another thread inserted same video just now
                        return videoRepo.findBySourceAndExternalVideoId(providerKey, externalVideoId)
                                .map(this::toDto);
                    }
                    return Mono.error(ex);
                });
    }

    private Mono<VideoResponse> importFallback(String provider, String externalId, Throwable t) {
        return Mono.error(new IllegalStateException(
                "Failed to import video " + externalId + " from " + provider, t));
    }

    private VideoResponse toDto(Video v) {
        return new VideoResponse(
                v.getId(), v.getTitle(), v.getSource(),
                v.getDurationMs(), v.getDescription(),
                v.getCategory(), v.getProvider(),
                v.getExternalVideoId(), v.getUploadDate(),
                v.getCreatedUserId()
        );
    }

    private ExternalVideo parseYoutube(ExternalYoutubeResponse resp) {
        var item    = resp.getItems().getFirst();
        String title = item.getSnippet().getTitle();
        String desc  = item.getSnippet().getDescription();
        long   durMs = parseIsoDuration(item.getContentDetails().getDuration());
        Instant published = item.getSnippet().getPublishedAt();
        return new ExternalVideo(title, desc, durMs, published, /*category*/0, /*provider*/1);
    }

    private ExternalVideo parseVimeo(ExternalVimeoResponse resp) {
        String title = resp.getName();
        String desc  = resp.getDescription();
        long   durMs = resp.getDuration() * 1_000;
        Instant published = resp.getReleaseTime();
        return new ExternalVideo(title, desc, durMs, published, 0, 2);
    }

    private long parseIsoDuration(String iso) {
        // parse PT#H#M#S → ms (you can use java.time.Duration)
        return java.time.Duration.parse(iso).toMillis();
    }

    private record ExternalVideo(
            String title, String description,
            long durationMs, Instant uploadDate,
            int categoryCode, int providerCode
    ) {}

}
