package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.web.dto.CachedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Central place to resolve the *current authenticated user's* numeric ID.
 * This is what the rest of the app should use.
 *
 * - Pulls Authentication from ReactiveSecurityContextHolder.
 * - Extracts a stable username (prefers auth.getName()).
 * - Uses the local cache (or DB fallback if you want) to get the numeric ID.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CurrentUserService {

    private final UserCacheService userCache;

    public Mono<Long> requireUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .switchIfEmpty(Mono.error(unauthorized("No security context")))
                .map(ctx -> ctx.getAuthentication())
                .switchIfEmpty(Mono.error(unauthorized("No authentication")))
                .flatMap(this::resolveUsernameFromAuth)      // Mono<String> username
                .flatMap(this::lookupUserIdByUsername)       // Mono<Long> userId
                .doOnNext(id -> log.debug("Resolved current userId={}", id));
    }

    /**
     * Extract a logical username from Authentication.
     * We prefer auth.getName() because Spring sets it consistently.
     * We fall back to Jwt sub if needed.
     */
    private Mono<String> resolveUsernameFromAuth(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return Mono.error(unauthorized("Unauthenticated"));
        }

        // Preferred: auth.getName()
        String name = auth.getName(); // e.g. "admin"
        if (name != null && !name.isBlank()) {
            return Mono.just(name);
        }

        // Fallback: if the principal is a Jwt, use its subject
        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String sub = jwt.getSubject();
            if (sub != null && !sub.isBlank()) {
                return Mono.just(sub);
            }
        }

        return Mono.error(unauthorized("Cannot resolve username"));
    }

    /**
     * Turn username → numeric id via local cache.
     * If cache miss, you can either:
     *   - call UserService / DB to fill it
     *   - or treat as unauthorized
     */
    private Mono<Long> lookupUserIdByUsername(String username) {
        return userCache
                .getUser(username) // Mono<CachedUser>
                .switchIfEmpty(Mono.error(unauthorized("User not cached: " + username)))
                .map(CachedUser::id);
    }

    private static RuntimeException unauthorized(String msg) {
//        return new IllegalStateException("Unauthorized: " + msg);
         return new ResponseStatusException(HttpStatus.UNAUTHORIZED, msg);
    }
}
