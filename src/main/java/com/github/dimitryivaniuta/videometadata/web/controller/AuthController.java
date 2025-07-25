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
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import com.github.dimitryivaniuta.videometadata.service.UserService;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.*;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;

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

/*    public Mono<ResponseEntity<TokenResponse>> login(@RequestBody AuthRequest request) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(request.getUsername(),
                        request.getPassword());

        return authManager.authenticate(authToken)
                .flatMap(this::issueToken)                   // on success, issue token
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .build()));
    }*/
/*
    private Mono<TokenResponse> issueToken(Authentication auth) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(3600);

        // collect roles
        String roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        // build claims
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("videometadata-service")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(auth.getName())
                .claim("roles", roles)
                .build();

        // encode synchronously, wrap in Mono
        return Mono.fromCallable(() -> {
                    Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims));
                    return new TokenResponse(jwt.getTokenValue(),
                            Objects.requireNonNull(jwt.getExpiresAt()).getEpochSecond());
                })
                // update lastLoginAt once the token is created
                .flatMap(tr -> userService.findByUsername(auth.getName())
                        .flatMap(u -> userService.updateLastLoginAt(u.id()))
                        .thenReturn(tr));
    }

    @Data
    public static class AuthRequest {
        private String username;
        private String password;
    }

    @Data
    public static class TokenResponse {
        private final String token;
        private final long   expiresAt;
    }*/
}

