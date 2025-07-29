package com.github.dimitryivaniuta.videometadata.graphql.security;

import com.github.dimitryivaniuta.videometadata.graphql.exceptions.GraphQlServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/** Reactive helpers to assert caller roles/scopes. */
public final class SecurityChecks {

    private SecurityChecks() {}

    public static Mono<Void> requireAnyRole(String... roles) {
        if (roles == null || roles.length == 0) return Mono.empty();

        final Set<String> required = Arrays.stream(roles)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        return ReactiveSecurityContextHolder.getContext()
                .map(sc -> sc.getAuthentication())
                .switchIfEmpty(Mono.error(new GraphQlServiceException("Unauthorized")))
                .flatMap(auth -> authorize(auth, required));
    }

    private static Mono<Void> authorize(Authentication auth, Set<String> required) {
        if (auth == null || !auth.isAuthenticated()) {
            return Mono.error(new GraphQlServiceException("Unauthorized"));
        }
        Set<String> have = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)   // e.g. ROLE_ADMIN
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        boolean ok = have.stream().anyMatch(required::contains);
        return ok
                ? Mono.empty()
                : Mono.error(new GraphQlServiceException(
                "Forbidden; requires one of " + required + ", but caller has " + have));
    }
}
