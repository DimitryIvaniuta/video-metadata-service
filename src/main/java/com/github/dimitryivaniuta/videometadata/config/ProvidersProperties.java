package com.github.dimitryivaniuta.videometadata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds all external video‑provider settings from video‑providers.yml.
 */
@Data
@ConfigurationProperties(prefix = "providers")
public class ProvidersProperties {

    private final YouTube youtube = new YouTube();
    private final Vimeo   vimeo   = new Vimeo();

    @Data
    public static class YouTube {
        /**
         * Your YouTube Data API v3 key.
         */
        private String apiKey;
        /**
         * Base URL (e.g. <a href="https://www.googleapis.com/youtube/v3">youtube</a>).
         */
        private String baseUrl;
    }

    @Data
    public static class Vimeo {
        /**
         * Bearer token for Vimeo REST API.
         */
        private String accessToken;
        /**
         * Base URL (e.g. <a href="https://api.vimeo.com">vimeo</a>).
         */
        private String baseUrl;
    }
}
