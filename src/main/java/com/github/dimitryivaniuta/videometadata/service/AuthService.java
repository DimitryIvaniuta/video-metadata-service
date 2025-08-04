package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.web.dto.CachedUser;
import com.github.dimitryivaniuta.videometadata.web.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String REFRESH_COOKIE = "REFRESH";

    private final ReactiveAuthenticationManager authManager;
    private final JwtTokenProvider              tokenProvider;
    private final UserService                   userService;
    private final UserCacheService              userCache;

    /* ───────────── LOGIN ───────────── */

/*
    public Mono<TokenResponse> login(String username, String password,
                                     ServerHttpResponse resp) {

        Authentication creds =
                new UsernamePasswordAuthenticationToken(username, password);

        return authManager.authenticate(creds)
                .flatMap(this::issueTokens)
                .doOnNext(pair -> setRefreshCookie(resp, pair.refresh()))
                .map(JwtTokenProvider.TokenPair::access)
                .onErrorMap(e -> new ResponseStatusException(UNAUTHORIZED, "Invalid credentials"));
    }
*/
public Mono<TokenResponse> login(String username,
                                 String password,
                                 ServerHttpResponse resp) {

    Authentication creds =
            new UsernamePasswordAuthenticationToken(username, password);

    /* 1 ─ authenticate  */
    return authManager.authenticate(creds)
            /* map ONLY auth errors */
            .onErrorMap(e -> new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid credentials"))

            /* 2 ─ issue access & refresh */
            .flatMap(auth -> issueTokens(auth)
                    /* set HttpOnly cookie */
                    .doOnNext(pair -> setRefreshCookie(resp, pair.refresh()))
                    .map(JwtTokenProvider.TokenPair::access));
}
    /* ───────────── REFRESH ──────────── */

    public Mono<TokenResponse> refresh(ServerHttpRequest req,
                                       ServerHttpResponse resp) {

        String refresh = req.getCookies().getFirst(REFRESH_COOKIE) != null
                ? req.getCookies().getFirst(REFRESH_COOKIE).getValue()
                : null;

        if (refresh == null || refresh.isBlank()) {
            return Mono.error(new ResponseStatusException(UNAUTHORIZED));
        }

        return tokenProvider.rotate(refresh)
                // keep the same cookie (update max-age)
                .doOnNext(tr -> setRefreshCookie(resp, refresh));
    }

    /* ───────────── HELPERS ──────────── */

    private Mono<JwtTokenProvider.TokenPair> issueTokens(Authentication auth) {
        String uname = auth.getName();

        return tokenProvider.issuePair(auth)
                .flatMap(pair ->
                        userService.findByUsername(uname)
                                .switchIfEmpty(Mono.error(new UsernameNotFoundException(uname)))
                                .flatMap(u -> userService.updateLastLoginAt(u.id()).thenReturn(u))
                                .flatMap(u -> userCache.cacheUser(CachedUser.of(u)))
                                .thenReturn(pair));
    }

    private static void setRefreshCookie(ServerHttpResponse resp, String token) {
        if (resp == null) {          // GraphQL invocation without HTTP response
            log.warn("No ServerHttpResponse – skipping REFRESH cookie");
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(true)                  // assume HTTPS
                .path("/")
                .maxAge(Duration.ofDays(7))    // same as refresh TTL
                .sameSite("Strict")
                .build();
        resp.addCookie(cookie);
    }
}
