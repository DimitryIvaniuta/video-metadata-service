package com.github.dimitryivaniuta.videometadata.handler;

import com.github.dimitryivaniuta.videometadata.domain.command.LoginCommand;
import com.github.dimitryivaniuta.videometadata.service.JwtTokenProvider;
import com.github.dimitryivaniuta.videometadata.service.UserService;
import com.github.dimitryivaniuta.videometadata.service.UserCacheService;
import com.github.dimitryivaniuta.videometadata.web.dto.CachedUser;
import com.github.dimitryivaniuta.videometadata.web.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Handles LoginCommand:
 * 1) authenticate credentials
 * 2) issue JWT
 * 3) update lastLoginAt
 * 4) cache user data
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginCommandHandler {

    private final ReactiveAuthenticationManager authManager;
    private final JwtTokenProvider              tokenProvider;
    private final UserService                   userService;
    private final UserCacheService              userCache;

    @CommandHandler
    public Mono<TokenResponse> handle(LoginCommand cmd) {
        return authManager.authenticate(cmd.toAuthToken())
                .flatMap(this::issueAndRecord)
                .doOnError(ex -> log.info("Login failed for user={}, cause={}",
                        cmd.getUsername(), ex.getMessage()));
    }

    private Mono<TokenResponse> issueAndRecord(Authentication auth) {
        String username = auth.getName();
        return tokenProvider.generateToken(auth)
                .flatMap(tr ->
                        userService.findByUsername(username)
                                .switchIfEmpty(Mono.error(new IllegalStateException("User not found")))
                                // 1) update lastLoginAt, then return the UserResponse
                                .flatMap(u -> userService.updateLastLoginAt(u.id())
                                        .thenReturn(u))
                                // 2) cache that user, then return the TokenResponse
                                .flatMap(updated ->
                                        userCache.cacheUser(CachedUser.of(updated))
                                                .thenReturn(tr)
                                )
                )
                .doOnSuccess(tr -> log.debug("Login successful for user={}", username));
    }
}
