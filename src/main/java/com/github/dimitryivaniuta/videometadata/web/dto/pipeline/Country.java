package com.github.dimitryivaniuta.videometadata.web.dto.pipeline;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Country DTO returned to the UI.
 */
@Schema(description = "Lightweight country representation")
public record Country(
        @Schema(example = "PL", description = "ISO 3166-1 alpha-2 country code")
        String code,
        @Schema(example = "Poland", description = "Display name")
        String name
) {}