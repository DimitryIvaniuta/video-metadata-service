package com.github.dimitryivaniuta.videometadata.web.dto;

import com.github.dimitryivaniuta.videometadata.model.Role;
import com.github.dimitryivaniuta.videometadata.model.User;
import com.github.dimitryivaniuta.videometadata.model.UserStatus;
import lombok.Builder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static com.github.dimitryivaniuta.videometadata.util.DateTimeUtil.toOffset;

/**
 * Representation of a user sent back to clients.
 */
@Builder
public record UserResponse(
        Long id,
        String username,
        String email,
        UserStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime lastLoginAt,
        Set<Role> roles
) {

    public static UserResponse toDto(User u, Set<Role> roles) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .status(u.getStatus())
                .createdAt(toOffset(u.getCreatedAt()))
                .updatedAt(toOffset(u.getUpdatedAt()))
                .lastLoginAt(toOffset(u.getLastLoginAt()))
                .roles(Set.copyOf(roles))
                .build();
    }

}