package com.github.dimitryivaniuta.videometadata.web.controller;

import com.github.dimitryivaniuta.videometadata.config.JwkKeyManager;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.util.JSONStringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/.well-known")
public class JwkSetController {

    private final JwkKeyManager keyManager;

    public JwkSetController(JwkKeyManager keyManager) {
        this.keyManager = keyManager;
    }

    @GetMapping(value = "/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        // Return the JSON string for compatibility with Spring MVC serialization
//JSONObjectUtils.toJSONString(keyManager.getPublicJwkSet().toJSONObject());
//        String jwks = keyManager.getPublicJwkSet().toJSONObject().toJSONString();
        JWKSet jwkSet = keyManager.getPublicJwkSet();
        return jwkSet.toJSONObject();
    }
}