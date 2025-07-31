package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.web.dto.TokenResponse;
import com.github.dimitryivaniuta.videometadata.web.dto.CachedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ReactiveAuthenticationManager authManager;
    private final JwtTokenProvider              tokenProvider;
    private final UserService                   userService;
    private final UserCacheService              userCacheService;

    /**
     * Authenticate, issue JWT, record lastLogin, cache the user, return token.
     */
    public Mono<TokenResponse> login(String username, String password) {
        return authManager.authenticate(
                        new UsernamePasswordAuthenticationToken(username, password)
                )
                .flatMap(this::issueAndRecord)
                .onErrorMap(e -> new IllegalArgumentException("Invalid credentials"));
    }

    private Mono<TokenResponse> issueAndRecord(Authentication auth) {
        String uname = auth.getName();
        return tokenProvider.generateToken(auth)
                .flatMap(token ->
                        userService.findByUsername(uname)
                                .switchIfEmpty(Mono.error(new UsernameNotFoundException(uname)))
                                // update lastLogin, then return the UserResponse
                                .flatMap(u -> userService.updateLastLoginAt(u.id()).thenReturn(u))
                                // cache the user, then return the TokenResponse
                                .flatMap(u -> userCacheService.cacheUser(CachedUser.of(u))
                                        .thenReturn(token))
                );
    }
}
