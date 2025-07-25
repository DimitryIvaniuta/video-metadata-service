package com.github.dimitryivaniuta.videometadata.web.dto;

import com.github.dimitryivaniuta.videometadata.model.UserStatus;

import java.time.Instant;
import java.util.Set;

/**
 * What we store in Redis for each logged‐in user.
 */
public record CachedUser(
        Long      id,
        String    username,
        String    email,
        UserStatus status,
        Instant   createdAt,
        Instant   updatedAt,
        Instant   lastLoginAt,
        Set<String> roles
) {}