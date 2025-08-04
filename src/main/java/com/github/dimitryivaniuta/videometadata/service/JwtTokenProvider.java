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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYP   = "typ";
    private static final String TYP_REFRESH = "refresh";

    private final JwkKeyManager            jwkKeyManager;
    private final JwtEncoder               jwtEncoder;
    private final ReactiveJwtDecoder       jwtDecoder;   // composite (RS + HS)
    private final SecurityJwtProperties    props;

    /* ------------- public API ------------------------------------------------ */

    /** Login -> issue access *and* refresh; caller decides what to return. */
    public Mono<TokenPair> issuePair(Authentication auth) {
        return Mono.zip(
                generateAccessToken(auth),
                generateRefreshToken(auth.getName())
        ).map(tuple -> new TokenPair(tuple.getT1(), tuple.getT2()));
    }

    /** Refresh exchange -> validate old refresh, mint new access. */
    public Mono<TokenResponse> rotate(String refreshToken) {
        return verifyRefresh(refreshToken)
                .flatMap(this::generateAccessToken);
    }

    /* ------------- access token --------------------------------------------- */

    public Mono<TokenResponse> generateAccessToken(Authentication auth) {
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .toList();
        return generateAccessToken(auth.getName(), roles);
    }

    public Mono<TokenResponse> generateAccessToken(String subject) {
        return generateAccessToken(subject, List.of());
    }

    private Mono<TokenResponse> generateAccessToken(String subject, List<String> roles) {
        Instant now  = Instant.now();
        Instant exp  = now.plusSeconds(props.getExpirationSeconds());

        JwtClaimsSet.Builder cb = commonClaims(subject, now, exp);
        if (!roles.isEmpty()) cb.claim(CLAIM_ROLES, roles);

        return encode(cb.build())
                .map(jwt -> new TokenResponse(jwt.getTokenValue(), jwt.getExpiresAt().getEpochSecond()));
    }

    /* ------------- refresh token -------------------------------------------- */

    private Mono<String> generateRefreshToken(String subject) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.getRefreshExpirationSeconds());

        JwtClaimsSet claims = commonClaims(subject, now, exp)
                .claim(CLAIM_TYP, TYP_REFRESH)
                .build();

        return encode(claims).map(Jwt::getTokenValue);
    }

    /** Verifies signature, exp, issuer, typ == "refresh"; returns subject. */
    private Mono<String> verifyRefresh(String token) {
        return jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    if (!TYP_REFRESH.equals(jwt.getClaimAsString(CLAIM_TYP))) {
                        return Mono.error(new JwtException("Not a refresh token"));
                    }
                    return Mono.just(jwt.getSubject());
                });
    }

    /* ------------- helpers --------------------------------------------------- */

    private JwtClaimsSet.Builder commonClaims(String sub, Instant iat, Instant exp) {
        JwtClaimsSet.Builder b = JwtClaimsSet.builder()
                .subject(sub)
                .issuer(props.getIssuer())
                .issuedAt(iat)
                .expiresAt(exp);
        if (props.getAudience() != null && !props.getAudience().isBlank()) {
            b.audience(List.of(props.getAudience()));
        }
        return b;
    }

    private Mono<Jwt> encode(JwtClaimsSet claims) {
        JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(jwkKeyManager.getCurrentKid())
                .build();
        return Mono.fromCallable(() -> jwtEncoder.encode(
                        JwtEncoderParameters.from(headers, claims)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /* ------------ DTO -------------------------------------------------------- */
    public record TokenPair(TokenResponse access, String refresh) {}
}


