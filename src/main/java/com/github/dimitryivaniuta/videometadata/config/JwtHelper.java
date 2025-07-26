package com.github.dimitryivaniuta.videometadata.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

/**
 * Lightweight helper to read a JWT header (the first Base64URL part)
 * without verifying the token signature.
 *
 * All methods are null/invalid-safe and NEVER throw checked exceptions.
 */
public final class JwtHelper {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE =
            new TypeReference<>() {};

    private JwtHelper() {
    }

    /**
     * Returns the JWT header as a Map or an empty Map if the token is invalid.
     */
    public static Map<String, Object> readHeader(final String token) {
        if (token == null || token.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return Collections.emptyMap();
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[0]);
            return MAPPER.readValue(decoded, MAP_TYPE);
        } catch (Exception ex) {
            // Swallow parsing errors and return empty — callers treat as unsupported token
            return Collections.emptyMap();
        }
    }

    /**
     * Reads the "alg" value from the JWT header, or empty string when absent/invalid.
     */
    public static String readAlg(final String token) {
        Object v = readHeader(token).get("alg");
        return v == null ? "" : v.toString();
    }

    /**
     * Reads the "kid" value from the JWT header, or empty string when absent/invalid.
     */
    public static String readKid(final String token) {
        Object v = readHeader(token).get("kid");
        return v == null ? "" : v.toString();
    }

    /**
     * Convenience: returns the raw (Base64URL-decoded) header JSON string,
     * or empty string on failure.
     */
    public static String readHeaderJson(final String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return "";
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[0]);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "";
        }
    }
}
