package com.github.dimitryivaniuta.videometadata.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class CompositeJwtDecoder implements ReactiveJwtDecoder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SecurityJwtProperties jwtProps;

    /** RS256 decoder (public keys fetched from /.well-known/jwks.json). */
    private final ReactiveJwtDecoder rs256Decoder;

    /** HS256 decoder (legacy shared secret). */
    private final ReactiveJwtDecoder hs256Decoder;

    public CompositeJwtDecoder(
            SecurityJwtProperties jwtProps,
            @Value("${security.jwt.jwks-uri:}") String configuredJwksUri,
            @Value("${application.base-url:http://localhost:8080}") String appBaseUrl
    ) {
        this.jwtProps = jwtProps;

        // Resolve JWKS URI (prefer explicit property; otherwise default to local endpoint)
        String jwksUri = (configuredJwksUri != null && !configuredJwksUri.isBlank())
                ? configuredJwksUri
                : appBaseUrl.replaceAll("/+$", "") + "/.well-known/jwks.json";

        // RS256 decoder against the JWKS URI
        NimbusReactiveJwtDecoder rsBuilder = NimbusReactiveJwtDecoder.withJwkSetUri(jwksUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        rsBuilder.setJwtValidator(buildValidator(jwtProps));
        this.rs256Decoder = rsBuilder;

        // HS256 decoder for migration period
        byte[] secretBytes = Base64.getDecoder().decode(jwtProps.getSecret());
        SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        NimbusReactiveJwtDecoder hsBuilder = NimbusReactiveJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        hsBuilder.setJwtValidator(buildValidator(jwtProps));
        this.hs256Decoder = hsBuilder;
    }

    @Override
    public Mono<Jwt> decode(String token) throws JwtException {
        return Mono.defer(() -> {
            String alg = readAlg(token);
            if ("RS256".equals(alg)) {
                return rs256Decoder.decode(token);
            } else if ("HS256".equals(alg)) {
                return hs256Decoder.decode(token);
            }
            return Mono.error(new JwtException("Unsupported JWT alg: " + alg));
        });
    }

    /**
     * Build a validator that enforces timestamp, issuer, audience, and skew.
     */
    private static OAuth2TokenValidator<Jwt> buildValidator(SecurityJwtProperties props) {
        Duration skew = Duration.ofSeconds(
                Math.max(0, props.getClockSkewSeconds())
        );

        OAuth2TokenValidator<Jwt> timestamp = new JwtTimestampValidator(skew);

        OAuth2TokenValidator<Jwt> issuer = (props.getIssuer() == null || props.getIssuer().isBlank())
                ? token -> OAuth2TokenValidatorResult.success()
                : new JwtIssuerValidator(props.getIssuer());

        OAuth2TokenValidator<Jwt> audience = (props.getAudience() == null || props.getAudience().isBlank())
                ? token -> OAuth2TokenValidatorResult.success()
                : (token -> {
            List<String> aud = token.getAudience();
            return (aud != null && aud.contains(props.getAudience()))
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token",
                            "Required audience not found: " + props.getAudience(),
                            null));
        });

        return new DelegatingOAuth2TokenValidator<>(timestamp, issuer, audience);
    }

    /**
     * Reads the 'alg' from the JWT header without verifying the token.
     */
    private static String readAlg(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return "";
            }
            byte[] headerBytes = Base64.getUrlDecoder().decode(parts[0]);
            Map<String, Object> header = MAPPER.readValue(headerBytes, new TypeReference<>() {});
            Object alg = header.get("alg");
            return alg == null ? "" : alg.toString();
        } catch (Exception e) {
            // fall back to empty -> unsupported
            return "";
        }
    }
}
