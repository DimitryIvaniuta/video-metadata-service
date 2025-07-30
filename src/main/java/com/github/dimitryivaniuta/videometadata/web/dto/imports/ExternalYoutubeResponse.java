package com.github.dimitryivaniuta.videometadata.web.dto.imports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.dimitryivaniuta.videometadata.model.VideoCategory;
import com.github.dimitryivaniuta.videometadata.model.VideoProvider;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Wrapper for YouTube Data API v3 videos.list response (subset).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalYoutubeResponse {

    @JsonProperty("items") private List<Item> items;

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JsonProperty("id")             private String id;
        @JsonProperty("snippet")        private Snippet snippet;
        @JsonProperty("contentDetails") private ContentDetails contentDetails;
        @JsonProperty("statistics")     private Statistics statistics;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Snippet {
        @JsonProperty("publishedAt") private Instant publishedAt;
        @JsonProperty("channelId")   private String channelId;
        @JsonProperty("title")       private String title;
        @JsonProperty("description") private String description;
        @JsonProperty("thumbnails")  private Thumbnails thumbnails;
        @JsonProperty("channelTitle") private String channelTitle;
        @JsonProperty("categoryId")   private String categoryId; // numeric string
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Thumbnails {
        @JsonProperty("default") private Thumbnail defaultThumbnail;
        @JsonProperty("medium")  private Thumbnail medium;
        @JsonProperty("high")    private Thumbnail high;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Thumbnail {
        @JsonProperty("url")    private String url;
        @JsonProperty("width")  private Integer width;
        @JsonProperty("height") private Integer height;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentDetails {
        /** ISO‑8601 duration, e.g. "PT1H2M10S" */
        @JsonProperty("duration")  private String duration;
        @JsonProperty("dimension") private String dimension;
        @JsonProperty("definition") private String definition;
        @JsonProperty("caption")    private String caption;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Statistics {
        @JsonProperty("viewCount")    private Long viewCount;
        @JsonProperty("likeCount")    private Long likeCount;
        @JsonProperty("commentCount") private Long commentCount;
    }

    public Metadata toMetadata(String videoId, String requestedBy) {
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("YouTube response has no items");
        }
        Item first = items.getFirst();
        Snippet sn = first.getSnippet();
        ContentDetails cd = first.getContentDetails();
        return Metadata.builder()
                .title(sn.getTitle())
                .description(sn.getDescription())
                .durationMs(parseDuration(cd.getDuration()))
                .videoCategory(VideoCategory.GENERAL)
                .videoProvider(VideoProvider.YOUTUBE)
                .externalVideoId(videoId)
                .uploadDate(sn.getPublishedAt())
                .requestedBy(requestedBy)
                .build();
    }

    private static Long parseDuration(String iso) {
        try {
            return (iso == null) ? null : Duration.parse(iso).toMillis();
        } catch (Exception ex) {
            return null;
        }
    }
}
