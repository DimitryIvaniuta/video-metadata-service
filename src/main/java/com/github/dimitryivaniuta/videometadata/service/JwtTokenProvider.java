package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.config.JwkKeyManager;
import com.github.dimitryivaniuta.videometadata.config.SecurityJwtProperties;
import com.github.dimitryivaniuta.videometadata.web.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwkKeyManager jwkKeyManager;
    private final JwtEncoder jwtEncoder;
    private final SecurityJwtProperties props;

    /**
     * Generates an HS256‐signed JWT in a fully reactive, non‑blocking way.
     */
    public Mono<TokenResponse> generateToken(Authentication auth) {
        Instant now     = Instant.now();
        Instant expires = now.plusSeconds(props.getExpirationSeconds());

        // 1) Build claims
        JwtClaimsSet.Builder cb = JwtClaimsSet.builder()
                .issuer(props.getIssuer())
                .issuedAt(now)
                .expiresAt(expires)
                .subject(auth.getName());

        if (props.getAudience() != null && !props.getAudience().isBlank()) {
            cb.audience(Collections.singletonList(props.getAudience()));
        }

        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)           // "ROLE_ADMIN"
                .map(name -> name.startsWith("ROLE_")
                        ? name.substring("ROLE_".length())         // "ADMIN"
                        : name)
                .collect(Collectors.toList());
        cb.claim("roles", roles);

        JwtClaimsSet claims = cb.build();

        // 2) Build a JWS header for HS256

        JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(jwkKeyManager.getCurrentKid())
                .build();
        log.info("Signing JWT with kid={}", jwkKeyManager.getCurrentKid());
        // 3) Encode on boundedElastic to avoid blocking the event‐loop
        JwtEncoderParameters params = JwtEncoderParameters.from(headers, claims);

        return Mono.fromCallable(() -> jwtEncoder.encode(params))
                .subscribeOn(Schedulers.boundedElastic())
                .map(jwt -> new TokenResponse(jwt.getTokenValue(), jwt.getExpiresAt().getEpochSecond()));

    }

}

