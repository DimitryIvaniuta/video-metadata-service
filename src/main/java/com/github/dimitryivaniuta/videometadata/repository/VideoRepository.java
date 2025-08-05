package com.github.dimitryivaniuta.videometadata.repository;

import com.github.dimitryivaniuta.videometadata.model.Video;
import com.github.dimitryivaniuta.videometadata.projection.CountRow;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.github.dimitryivaniuta.videometadata.model.VideoProvider;

public interface VideoRepository extends ReactiveCrudRepository<Video, Long> {

    Mono<Video> findByProviderAndExternalVideoId(VideoProvider provider, String externalVideoId);

    /* ---- Count with optional provider filter ---- */
/*    @Query("""
           SELECT COUNT(*) 
           FROM videos v
           WHERE (:provider IS NULL OR :provider = '' OR v.source = :provider)
           """)
    Mono<Long> countByProviderNullable(String provider);*/
    Mono<Long> count();
    Mono<Long> countBySource(String source);
//    Mono<Long> countByProvider(VideoProvider provider);
    @Query("""
            SELECT COUNT(*)::bigint AS cnt
            FROM videos v
            WHERE (:provider IS NULL OR v.provider = :provider)
            """)
    Mono<CountRow> countByProviderNullable(@Param("provider") VideoProvider provider);

    /* ---- Page: created_at (imported_at) ---- */

    @Query("""
           SELECT id, title, source, duration_ms, description, category, provider,
                  external_video_id, upload_date, created_user_id
           FROM videos v
           WHERE (:provider IS NULL OR v.provider = :provider)
           ORDER BY v.id DESC
           OFFSET :offset LIMIT :limit
           """)
    Flux<Video> pageOrderByImportedAtDesc(@Param("provider") VideoProvider provider,
                                          @Param("limit") long limit,
                                          @Param("offset") long offset);

    @Query("""
           SELECT id, title, source, duration_ms, description, category, provider,
                  external_video_id, upload_date, created_user_id
           FROM videos v
           WHERE (:provider IS NULL OR v.provider = :provider)
           ORDER BY v.id ASC
           OFFSET :offset LIMIT :limit
           """)
    Flux<Video> pageOrderByImportedAtAsc(@Param("provider") VideoProvider provider,
                                         @Param("limit") long limit,
                                         @Param("offset") long offset);

    @Query("""
           SELECT id, title, source, duration_ms, description, category, provider,
                  external_video_id, upload_date, created_user_id
           FROM videos v
           WHERE (:provider IS NULL OR v.provider = :provider)
           ORDER BY v.upload_date DESC NULLS LAST
           OFFSET :offset LIMIT :limit
           """)
    Flux<Video> pageOrderByUploadDateDesc(@Param("provider") VideoProvider provider,
                                          @Param("limit") long limit,
                                          @Param("offset") long offset);

    @Query("""
           SELECT id, title, source, duration_ms, description, category, provider,
                  external_video_id, upload_date, created_user_id
           FROM videos v
           WHERE (:provider IS NULL OR v.provider = :provider)
           ORDER BY v.upload_date ASC NULLS FIRST
           OFFSET :offset LIMIT :limit
           """)
    Flux<Video> pageOrderByUploadDateAsc(@Param("provider") VideoProvider provider,
                                         @Param("limit") long limit,
                                         @Param("offset") long offset);

    @Query("""
           SELECT id, title, source, duration_ms, description, category, provider,
                  external_video_id, upload_date, created_user_id
           FROM videos v
           WHERE (:provider IS NULL OR v.provider = :provider)
           ORDER BY v.title DESC NULLS LAST
           OFFSET :offset LIMIT :limit
           """)
    Flux<Video> pageOrderByTitleDesc(@Param("provider") VideoProvider provider,
                                     @Param("limit") long limit,
                                     @Param("offset") long offset);

    @Query("""
           SELECT id, title, source, duration_ms, description, category, provider,
                  external_video_id, upload_date, created_user_id
           FROM videos v
           WHERE (:provider IS NULL OR v.provider = :provider)
           ORDER BY v.title ASC NULLS FIRST
           OFFSET :offset LIMIT :limit
           """)
    Flux<Video> pageOrderByTitleAsc(@Param("provider") VideoProvider provider,
                                    @Param("limit") long limit,
                                    @Param("offset") long offset);

    @Query("""
           SELECT v.* FROM videos v
           WHERE (:provider IS NULL OR :provider = '' OR v.source = :provider)
           ORDER BY v.created_at DESC
           LIMIT :limit OFFSET :offset
           """)
    Flux<Video> pageByProviderOrderByCreatedAtDesc(String provider, long limit, long offset);

    @Query("""
           SELECT v.* FROM videos v
           WHERE (:provider IS NULL OR :provider = '' OR v.source = :provider)
           ORDER BY v.created_at ASC
           LIMIT :limit OFFSET :offset
           """)
    Flux<Video> pageByProviderOrderByCreatedAtAsc(String provider, long limit, long offset);

    /* ---- Page: upload_date ---- */
    @Query("""
           SELECT v.* FROM videos v
           WHERE (:provider IS NULL OR :provider = '' OR v.source = :provider)
           ORDER BY v.upload_date DESC
           LIMIT :limit OFFSET :offset
           """)
    Flux<Video> pageByProviderOrderByUploadDateDesc(String provider, long limit, long offset);

    @Query("""
           SELECT v.* FROM videos v
           WHERE (:provider IS NULL OR :provider = '' OR v.source = :provider)
           ORDER BY v.upload_date ASC
           LIMIT :limit OFFSET :offset
           """)
    Flux<Video> pageByProviderOrderByUploadDateAsc(String provider, long limit, long offset);

    /* ---- Page: title ---- */
    @Query("""
           SELECT v.* FROM videos v
           WHERE (:provider IS NULL OR :provider = '' OR v.source = :provider)
           ORDER BY v.title DESC
           LIMIT :limit OFFSET :offset
           """)
    Flux<Video> pageByProviderOrderByTitleDesc(String provider, long limit, long offset);

    @Query("""
           SELECT v.* FROM videos v
           WHERE (:provider IS NULL OR :provider = '' OR v.source = :provider)
           ORDER BY v.title ASC
           LIMIT :limit OFFSET :offset
           """)
    Flux<Video> pageByProviderOrderByTitleAsc(String provider, long limit, long offset);
}