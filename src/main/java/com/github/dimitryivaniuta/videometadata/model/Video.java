package com.github.dimitryivaniuta.videometadata.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.*;

import java.time.Clock;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("videos")
public class Video {

    @Id
    private Long id;

    private String title;

    private String source;

    @Column("duration_ms")
    private Long durationMs;

    private String description;

    @Column("category")
    private VideoCategory       category;

    @Column("provider")
    private VideoProvider       provider;

    @Column("external_video_id")
    private String externalVideoId;

    @Column("upload_date")
    private Instant uploadDate;

    @Column("created_at")
    private Instant createdAt;

    @Column("created_user_id")
    private Long createdUserId;
}
