package com.github.dimitryivaniuta.videometadata.web.dto;

import com.github.dimitryivaniuta.videometadata.model.Role;
import com.github.dimitryivaniuta.videometadata.model.UserStatus;
import lombok.Builder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * What we store in Redis for each logged‐in user.
 */
@Builder
public record CachedUser(
        Long      id,
        String    username,
        String    email,
        UserStatus status,
        Instant   createdAt,
        Instant   updatedAt,
        Instant   lastLoginAt,
        Set<Role> roles
) {
    /**
     * Build a CachedUser from your UserResponse.
     */
    public static CachedUser of(UserResponse u) {
        return CachedUser.builder()
                .id(u.id())
                .username(u.username())
                .email(u.email())
                .status(u.status())
                .createdAt(toInstant(u.createdAt()))
                .updatedAt(toInstant(u.updatedAt()))
                .lastLoginAt(toInstant(u.lastLoginAt()))
                .roles(Set.copyOf(u.roles()))
                .build();
    }

    private static Instant toInstant(OffsetDateTime odt) {
        return odt != null ? odt.toInstant() : null;
    }
}