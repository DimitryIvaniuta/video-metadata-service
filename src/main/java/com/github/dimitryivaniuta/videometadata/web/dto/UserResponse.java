package com.github.dimitryivaniuta.videometadata.web.dto;

import com.github.dimitryivaniuta.videometadata.model.Role;
import com.github.dimitryivaniuta.videometadata.model.User;
import com.github.dimitryivaniuta.videometadata.model.UserStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
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
        Set<Role> roles
) {

    public static UserResponse toDto(User u, List<Role> roles) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .status(u.getStatus())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .lastLoginAt(u.getLastLoginAt())
                .roles(Set.copyOf(roles))
                .build();
    }
}