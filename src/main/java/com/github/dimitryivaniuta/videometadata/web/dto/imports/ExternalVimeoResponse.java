package com.github.dimitryivaniuta.videometadata.web.dto.imports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalVimeoResponse {

    @JsonProperty("uri")
    private String uri;                // e.g. "/videos/12345678"

    @JsonProperty("name")
    private String name;               // video title

    @JsonProperty("description")
    private String description;

    /**
     * Duration in seconds.
     */
    @JsonProperty("duration")
    private Integer duration;

    @JsonProperty("release_time")
    private Instant releaseTime;       // upload date

    @JsonProperty("width")
    private Integer width;

    @JsonProperty("height")
    private Integer height;

    @JsonProperty("language")
    private String language;

    @JsonProperty("tags")
    private List<Tag> tags;

    @JsonProperty("stats")
    private Stats stats;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tag {
        @JsonProperty("uri")
        private String uri;

        @JsonProperty("name")
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Stats {
        @JsonProperty("plays")
        private Long plays;
    }

    // You can add more fields (pictures, user, license) as needed
}

