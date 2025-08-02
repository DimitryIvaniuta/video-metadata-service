package com.github.dimitryivaniuta.videometadata.web.dto.imports;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelId(
        @JsonProperty("channelId") String channelId) {}