package com.github.dimitryivaniuta.videometadata.web.dto.imports;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Channel search -> channelId */
public record SearchChannelResponse(
        @JsonProperty("items") List<SearchChannelItem> items) {}