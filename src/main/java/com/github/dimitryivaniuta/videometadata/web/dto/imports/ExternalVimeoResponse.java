package com.github.dimitryivaniuta.videometadata.web.dto.imports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.dimitryivaniuta.videometadata.model.VideoCategory;
import com.github.dimitryivaniuta.videometadata.model.VideoProvider;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * DTO for Vimeo /videos/{id} response (subset + extra fields you already had).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalVimeoResponse {

    @JsonProperty("uri")           private String uri;          // "/videos/12345678"
    @JsonProperty("name")          private String name;         // title
    @JsonProperty("description")   private String description;
    @JsonProperty("duration")      private Integer duration;    // seconds
    @JsonProperty("release_time")  private Instant releaseTime; // upload date
    @JsonProperty("width")         private Integer width;
    @JsonProperty("height")        private Integer height;
    @JsonProperty("language")      private String language;
    @JsonProperty("tags")          private List<Tag> tags;
    @JsonProperty("stats")         private Stats stats;

    /* -------- nested -------- */
    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tag {
        @JsonProperty("uri")  private String uri;
        @JsonProperty("name") private String name;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Stats {
        @JsonProperty("plays") private Long plays;
    }

    public Metadata toMetadata(String videoId, String requestedBy) {
        return Metadata.builder()
                .title(name)
                .description(description)
                .durationMs(duration != null ? duration * 1_000L : null)
                .videoCategory(VideoCategory.GENERAL)
                .videoProvider(VideoProvider.YOUTUBE)
                .externalVideoId(videoId)
                .uploadDate(releaseTime)
                .requestedBy(requestedBy)
                .build();
    }
}

