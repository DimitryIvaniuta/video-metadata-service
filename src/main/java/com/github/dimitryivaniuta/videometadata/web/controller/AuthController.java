package com.github.dimitryivaniuta.videometadata.web.controller;

import com.github.dimitryivaniuta.videometadata.service.JwtTokenProvider;
import com.github.dimitryivaniuta.videometadata.service.UserCacheService;
import com.github.dimitryivaniuta.videometadata.service.UserService;
import com.github.dimitryivaniuta.videometadata.web.dto.AuthRequest;
import com.github.dimitryivaniuta.videometadata.web.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final ReactiveAuthenticationManager authManager;
    private final JwtTokenProvider            tokenProvider;
    private final UserService                 userService;
    private final UserCacheService            userCacheService;

    @PostMapping("/login")
    public Mono<ResponseEntity<TokenResponse>> login(
            @Valid @RequestBody AuthRequest req) {

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(req.username(), req.password());

        return authManager.authenticate(authToken)
                .flatMap(this::issueRecordAndCache)
                .map(ResponseEntity::ok)
                .onErrorResume(AuthenticationException.class,
                        e -> Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
                );
    }

    private Mono<TokenResponse> issueRecordAndCache(Authentication auth) {
        // 1) Generate JWT
        Mono<TokenResponse> tokenMono = tokenProvider.generateToken(auth);

        // 2) Prepare the caching side‑effect
        Mono<Void> cacheMono = userService.findByUsername(auth.getName())
                .switchIfEmpty(Mono.error(new UsernameNotFoundException(auth.getName())))
                .flatMap(u -> userService.updateLastLoginAt(u.id()).thenReturn(u))
                .flatMap(u -> userCacheService.getUser(u.username()).then())
                .then();

        // 3) Sequence: generate token, then run cacheMono, then return the token
        return tokenMono.flatMap(cacheMono::thenReturn);
    }
}
