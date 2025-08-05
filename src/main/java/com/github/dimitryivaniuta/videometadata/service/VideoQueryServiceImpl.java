package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.model.User;
import com.github.dimitryivaniuta.videometadata.model.Video;
import com.github.dimitryivaniuta.videometadata.model.VideoProvider;
import com.github.dimitryivaniuta.videometadata.projection.CountRow;
import com.github.dimitryivaniuta.videometadata.repository.VideoRepository;
import com.github.dimitryivaniuta.videometadata.web.dto.VideoConnection;
import com.github.dimitryivaniuta.videometadata.web.dto.graphql.types.VideoSort;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.VideoResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VideoQueryServiceImpl implements VideoQueryService {

    private static final int DEFAULT_PAGE = 1;

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MAX_PAGE_SIZE = 100;

    private final VideoRepository videoRepo;

    /*
        @Override
        public Mono<VideoConnection> fetchVideos(Integer page, Integer pageSize, String provider, VideoSort sortBy, Boolean sortDesc) {
            Objects.requireNonNull(provider, "provider must not be null");
            final int p = normalizePage(page);
            final int s = normalizePageSize(pageSize);
            final long offset = (long) (p - 1) * s;
            final boolean desc = sortDesc == null || sortDesc;
            final VideoSort sort = (sortBy == null ? VideoSort.IMPORTED_AT : sortBy);

            Mono<Long> totalMono = StringUtils.isBlank(provider)
                    ? videoRepo.count()
                    : videoRepo.countByProvider(VideoProvider.valueOf(provider));

            Flux<Video> pageFlux =  switch (sort) {
                case IMPORTED_AT -> desc
                        ? videoRepo.pageByProviderOrderByCreatedAtDesc(nullIfBlank(provider), s, offset)
                        : videoRepo.pageByProviderOrderByCreatedAtAsc(nullIfBlank(provider), s, offset);
                case UPLOAD_DATE -> desc
                        ? videoRepo.pageByProviderOrderByUploadDateDesc(nullIfBlank(provider), s, offset)
                        : videoRepo.pageByProviderOrderByUploadDateAsc(nullIfBlank(provider), s, offset);
                case TITLE -> desc
                        ? videoRepo.pageByProviderOrderByTitleDesc(nullIfBlank(provider), s, offset)
                        : videoRepo.pageByProviderOrderByTitleAsc(nullIfBlank(provider), s, offset);
            };

            return pageFlux
                    .map(VideoResponse::toDto)
                    .collectList()
                    .zipWith(totalMono)
                    .map(t -> VideoConnection.builder()
                            .items(t.getT1())
                            .page(p)
                            .pageSize(s)
                            .total(t.getT2())
                            .build());
        }*/
    @Override
    public Mono<VideoConnection> fetchVideos(Integer page,
                                             Integer pageSize,
                                             String provider,
                                             VideoSort sortBy,
                                             Boolean sortDesc) {

        final int p = normalizePage(page);
        final int s = normalizePageSize(pageSize);
        final long offset = (long) (p - 1) * s;
        final boolean desc = (sortDesc == null || sortDesc);
        final VideoSort sort = (sortBy == null ? VideoSort.IMPORTED_AT : sortBy);

        final VideoProvider providerEnum = parseProviderOrNull(provider);

        Mono<Long> totalMono = videoRepo.countByProviderNullable(providerEnum)
                .map(CountRow::cnt)
                .defaultIfEmpty(0L);

        var pageFlux = switch (sort) {
            case IMPORTED_AT -> desc
                    ? videoRepo.pageOrderByImportedAtDesc(providerEnum, s, offset)
                    : videoRepo.pageOrderByImportedAtAsc(providerEnum, s, offset);
            case UPLOAD_DATE -> desc
                    ? videoRepo.pageOrderByUploadDateDesc(providerEnum, s, offset)
                    : videoRepo.pageOrderByUploadDateAsc(providerEnum, s, offset);
            case TITLE -> desc
                    ? videoRepo.pageOrderByTitleDesc(providerEnum, s, offset)
                    : videoRepo.pageOrderByTitleAsc(providerEnum, s, offset);
        };

        return pageFlux
                .map(VideoResponse::toDto)
                .collectList()
                .zipWith(totalMono)
                .map(t -> VideoConnection.builder()
                        .items(t.getT1())
                        .page(p)
                        .pageSize(s)
                        .total(t.getT2())
                        .build());
    }


    private static VideoProvider parseProviderOrNull(String provider) {
        if (provider == null || provider.isBlank()) return null;
        try {
            return VideoProvider.valueOf(provider.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown provider: " + provider);
        }
    }

    @Override
    public Mono<Long> countVideos(String provider) {
        Objects.requireNonNull(provider, "provider must not be null");
//        return videoRepo.countByProviderNullable(VideoProvider.valueOf(provider));
        return videoRepo.countByProviderNullable(VideoProvider.valueOf(provider))
                .map(CountRow::cnt)
                .defaultIfEmpty(0L);
    }

    private static int normalizePage(Integer page) {
        return (page == null || page < 1) ? DEFAULT_PAGE : page;
    }

    private static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) return DEFAULT_PAGE_SIZE;
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private static String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
