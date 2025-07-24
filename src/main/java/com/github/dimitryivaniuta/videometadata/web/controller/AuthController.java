package com.github.dimitryivaniuta.videometadata.web.controller;

import com.github.dimitryivaniuta.videometadata.web.dto.TokenResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final ReactiveAuthenticationManager authManager;
    private final ReactiveJwtEncoder jwtEncoder;

    @PostMapping("/login")
    public Mono<ResponseEntity<TokenResponse>> login(@RequestBody AuthRequest req) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword());

        return authManager.authenticate(authToken)
                .flatMap(auth -> {
                    // build and encode token as before...
                    Mono<TokenResponse> tokenMono = jwtEncoder
                            .encode(JwtEncoderParameters.from(claims))
                            .map(jwt -> new TokenResponse(jwt.getTokenValue(), exp));

                    // update lastLoginAt on success
                    return userService.findByUsername(auth.getName())
                            .flatMap(u -> userService.updateLastLoginAt(u.getId()))
                            .then(tokenMono);
                })
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

}
