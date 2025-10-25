package com.github.dimitryivaniuta.videometadata.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

/**
 * Helper to pull the current authenticated user id (Long) from reactive context.
 */
public final class CurrentUser {
    private CurrentUser() {}

    public static Mono<Long> requireUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(CurrentUser::extractUserId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Unauthenticated")));
    }

    private static Mono<Long> extractUserId(Authentication auth) {
        if (auth == null) {
            return Mono.error(new IllegalStateException("Unauthenticated"));
        }

        Object principal = auth.getPrincipal();

        // Case 1: Spring Security OAuth2 resource server style, principal is Jwt
        if (principal instanceof Jwt jwt) {
            String sub = jwt.getSubject(); // "sub" claim
            try {
                return Mono.just(Long.parseLong(sub));
            } catch (NumberFormatException nfe) {
                return Mono.error(new IllegalStateException("JWT sub is not numeric: " + sub));
            }
        }

        // Case 2: You might have set auth.getName() to username or id in a custom auth manager.
        // If you stored numeric user id in auth.getName(), handle it:
        String name = auth.getName();
        try {
            return Mono.just(Long.parseLong(name));
        } catch (NumberFormatException nfe) {
            // If it's a username, you could look up userId by username instead
            return Mono.error(new IllegalStateException("Cannot resolve user id from principal"));
        }
    }
}
