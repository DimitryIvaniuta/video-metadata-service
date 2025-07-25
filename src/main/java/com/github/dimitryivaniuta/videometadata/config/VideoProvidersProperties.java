package com.github.dimitryivaniuta.videometadata.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Binds all external video‑provider settings from video‑providers.yml.
 */
@Getter
@Setter
@Configuration
//@Data
@ConfigurationProperties(prefix = "video")
public class VideoProvidersProperties {
    private Map<String, Provider> providers;

    @Data
    public static class Provider {
        private String apiKey;
        private String accessToken;
        private String baseUrl;
    }

}
