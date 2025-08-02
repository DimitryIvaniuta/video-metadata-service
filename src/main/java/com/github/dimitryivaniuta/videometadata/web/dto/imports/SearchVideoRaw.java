package com.github.dimitryivaniuta.videometadata.web.dto.imports;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Raw search video page from YouTube */
public record SearchVideoRaw(
        @JsonProperty("items") List<SearchVideoItem> items,
        @JsonProperty("nextPageToken") String nextPageToken) {}
