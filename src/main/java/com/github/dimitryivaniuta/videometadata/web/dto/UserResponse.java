package com.github.dimitryivaniuta.videometadata.web.dto;

import com.github.dimitryivaniuta.videometadata.model.UserStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.Set;

/**
 * Representation of a user sent back to clients.
 */
@Builder
public record UserResponse(
        Long id,
        String username,
        String email,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLoginAt,
        Set<String> roles
) {
}