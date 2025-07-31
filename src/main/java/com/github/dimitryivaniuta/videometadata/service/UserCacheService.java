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

    private static final String PREFIX = "user:";

    private final ReactiveRedisTemplate<String, CachedUser> redis;
    private final UserService                               userService;
    private final SecurityJwtProperties jwtProperties;

    private String cacheKey(String username) {
        return PREFIX + username;
    }

    /**
     * Retrieves user details from Redis if present; otherwise
     * fetches from the UserService (DB), caches it, and returns.
     * If missing, load from UserService, cache it with TTL, then return.
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

    /**
     * Cache the given user (e.g. after login) under "user:{username}".
     */
    public Mono<Void> cacheUser(CachedUser user) {
        String key = cacheKey(user.username());
        return redis.opsForValue()
                .set(key, user, Duration.ofSeconds(jwtProperties.getExpirationSeconds()))
                .then();
    }

    /**
     * Evict the cached user, forcing a DB reload on next getUser().
     */
    public Mono<Boolean> evict(String username) {
        return redis.opsForValue().delete(cacheKey(username));
    }

    private CachedUser toCached(UserResponse u) {
        return CachedUser.of(u);
    }

}
