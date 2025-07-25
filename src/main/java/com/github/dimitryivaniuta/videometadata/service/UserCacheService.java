package com.github.dimitryivaniuta.videometadata.service;


import com.github.dimitryivaniuta.videometadata.config.SecurityJwtProperties;
import com.github.dimitryivaniuta.videometadata.web.dto.CachedUser;
import com.github.dimitryivaniuta.videometadata.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final ReactiveRedisTemplate<String, CachedUser> redis;
    private final UserService                               userService;
    private final SecurityJwtProperties jwtProperties;

    /** TTL for cached user entries, in seconds (match your JWT expiration). */
//    @Value("${security.jwt.expiration-seconds:3600}")
//    private long cacheTtlSec;

    private String cacheKey(String username) {
        return "user:" + username;
    }

    /**
     * Retrieves user details from Redis if present; otherwise
     * fetches from the UserService (DB), caches it, and returns.
     */
    public Mono<CachedUser> getUser(String username) {
        String key = cacheKey(username);
        return redis.opsForValue().get(key)
                .switchIfEmpty(
                        userService.findByUsername(username)
                                .map(this::toCached)
                                .flatMap(cu ->
                                        redis.opsForValue()
                                                .set(key, cu, Duration.ofSeconds(jwtProperties.getExpirationSeconds()))
                                                .thenReturn(cu)
                                )
                );
    }

    private CachedUser toCached(UserResponse u) {
        return new CachedUser(
                u.id(), u.username(), u.email(), u.status(),
                u.createdAt(), u.updatedAt(), u.lastLoginAt(),
                u.roles()
        );
    }

    /** Invalidate cache for a given username (e.g. on password change). */
    public Mono<Boolean> evict(String username) {
        return redis.opsForValue().delete(cacheKey(username));
    }
}
