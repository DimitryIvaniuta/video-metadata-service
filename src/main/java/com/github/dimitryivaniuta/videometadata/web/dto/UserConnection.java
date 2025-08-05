package com.github.dimitryivaniuta.videometadata.web.dto;

import lombok.Builder;

import java.util.List;

/**
 * Paged result for users.
 */
@Builder
public record UserConnection(
        List<UserResponse> items,
        int page,
        int pageSize,
        long total
) {}