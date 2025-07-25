package com.github.dimitryivaniuta.videometadata.web.controller;

import com.github.dimitryivaniuta.videometadata.service.JwtTokenProvider;
import com.github.dimitryivaniuta.videometadata.web.dto.AuthRequest;
import com.github.dimitryivaniuta.videometadata.web.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import com.github.dimitryivaniuta.videometadata.service.UserService;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final ReactiveAuthenticationManager authManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;

    @PostMapping("/login")
    public Mono<ResponseEntity<TokenResponse>> login(
            @Valid @RequestBody AuthRequest req) {

        var authToken = new UsernamePasswordAuthenticationToken(
                req.username(), req.password()
        );

        return authManager.authenticate(authToken)
                .flatMap(this::issueAndRecord)
                .map(ResponseEntity::ok)
                .onErrorResume(AuthenticationException.class,
                        e -> Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
                );
    }

    private Mono<TokenResponse> issueAndRecord(Authentication auth) {
        return tokenProvider.generateToken(auth)
                .flatMap(tr -> userService.findByUsername(auth.getName())
                        .switchIfEmpty(Mono.error(new UsernameNotFoundException(auth.getName())))
                        .flatMap(u -> userService.updateLastLoginAt(u.id()))
                        .thenReturn(tr)
                );
    }

}

