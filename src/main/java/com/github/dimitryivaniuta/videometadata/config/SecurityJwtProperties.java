package com.github.dimitryivaniuta.videometadata.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the properties under security.jwt.* for your own token generation and validation.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "security.jwt")
public class SecurityJwtProperties {

    /**
     * The issuer (iss claim) for all JWTs your service generates.
     */
    private String issuer;

    /**
     * The audience (aud claim) your tokens are intended for.
     */
    private String audience;

    /**
     * Base‑64 encoded HMAC‑SHA256 secret (HS256).
     */
    private String secret;

    /**
     * How long (seconds) until an access token expires.
     */
    private long expirationSeconds;

    /**
     * How long (seconds) until a refresh token expires.
     */
    private long refreshExpirationSeconds;

    /**
     * Allowed clock skew (seconds) when validating incoming JWTs.
     */
    private long clockSkewSeconds;
}
