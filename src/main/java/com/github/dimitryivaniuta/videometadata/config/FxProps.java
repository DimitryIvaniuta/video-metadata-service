package com.github.dimitryivaniuta.videometadata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fx")
public record FxProps(
        String base,
        String symbols,
        Provider provider,
        String apiKey
) {
    public record Provider(String url, int timeoutMs) {}
}