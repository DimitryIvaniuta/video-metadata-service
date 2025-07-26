package com.github.dimitryivaniuta.videometadata.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds a rotating set of RSA JWKs. New key every rotationPeriod,
 * old keys retained for retirePeriod so existing tokens validate.
 */
@Component
@Slf4j
@EnableScheduling
public class JwkKeyManager {

    /** Active keys mapped by kid */
    private final Map<String, RSAKey> keys = new ConcurrentHashMap<>();

    /** The current signing key's kid */
    @Getter private volatile String currentKid;

    /** kid -> createdAt */
    private final Map<String, Instant> createdAt = new ConcurrentHashMap<>();

    private final Duration rotationPeriod = Duration.ofDays(1);
    private final Duration retirePeriod   = Duration.ofDays(7);

    public JwkKeyManager() {
        rotateKeys();
    }

    @Scheduled(fixedRateString = "${jwt.rotation-period-ms:86400000}")
    public void rotateKeys() {
        rotateNow();
        retireOld();
    }

    /** Generate a new RSA key and retire aged ones */
    public void rotateNow() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair kp = gen.generateKeyPair();

            String kid = UUID.randomUUID().toString();
            RSAKey rsaJwk = new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                    .privateKey((RSAPrivateKey) kp.getPrivate())
                    .algorithm(JWSAlgorithm.RS256)
                    .keyID(kid)
                    .keyUse(KeyUse.SIGNATURE)
                    .build();

            keys.put(kid, rsaJwk);
            createdAt.put(kid, Instant.now());
            currentKid = kid;
            log.info("Rotated JWK; new currentKid={}", kid);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA key generation failed", e);
        }
    }

    private void retireOld() {
        Instant cutoff = Instant.now().minus(retirePeriod);
        keys.keySet().removeIf(kid -> {
            Instant ts = createdAt.getOrDefault(kid, Instant.EPOCH);
            if (!kid.equals(currentKid) && ts.isBefore(cutoff)) {
                log.info("Retiring RSA JWK kid={}", kid);
                createdAt.remove(kid);
                return true;
            }
            return false;
        });
    }


    /** For signing: returns a JWKSource containing PRIVATE keys (never expose externally). */
    public ImmutableJWKSet<SecurityContext> getSigningJwkSource() {
        // Do NOT strip private parts here
        List<JWK> priv = new ArrayList<>(keys.values());
        return new ImmutableJWKSet<>(new JWKSet(priv));
    }

    /** For publishing: only PUBLIC JWKs (safe to expose at /.well-known/jwks.json). */
    public JWKSet getPublicJwkSet() {
        List<JWK> publics = keys.values().stream()
                .map(RSAKey::toPublicJWK)
                .map(jwk -> (JWK) jwk)
                .toList();
        return new JWKSet(publics);
    }

    /** Returns a JWKSource backed by the rotating keys for signing/verification */
    public ImmutableJWKSet<SecurityContext> getJwkSource() {
        return new ImmutableJWKSet<>(getPublicJwkSet());
    }


    /** Dynamic JWKSource that always returns the CURRENT private RSA key for signing. */
    public JWKSource<SecurityContext> getSigningJwkSourceDynamic() {
        return (jwkSelector, securityContext) -> {
            RSAKey current = keys.get(currentKid);
            if (current == null) {
                return java.util.Collections.emptyList();
            }
            // Return the current key even if the selector is overly strict.
            // NimbusJwtEncoder will use the first returned.
            return java.util.List.of(current);
        };
    }

}
