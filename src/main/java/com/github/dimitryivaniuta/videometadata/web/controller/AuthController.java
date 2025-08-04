package com.github.dimitryivaniuta.videometadata.web.controller;

import com.github.dimitryivaniuta.videometadata.service.AuthService;
import com.github.dimitryivaniuta.videometadata.web.dto.AuthRequest;
import com.github.dimitryivaniuta.videometadata.web.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Mono<TokenResponse> login(@Valid @RequestBody AuthRequest body,
                                     ServerHttpResponse resp) {
        return authService.login(body.username(), body.password(), resp);
    }

    @PostMapping("/refresh")
    public Mono<TokenResponse> refresh(ServerHttpRequest req,
                                       ServerHttpResponse resp) {
        return authService.refresh(req, resp);
    }
}
