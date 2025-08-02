package com.github.dimitryivaniuta.videometadata.graphql.schema;

import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Active only while the `printSchema` Gradle task runs.
 * Provides a dummy CacheManager so CacheAutoConfigurationValidator
 * is satisfied even though Redis is absent.
 */
@Configuration
@Profile("schema-print")
public class SchemaPrintCacheStub {

    @Bean            // <<< the missing bean
    public CacheManager cacheManager() {
        return new NoOpCacheManager();   // does nothing, never used during printing
    }
}