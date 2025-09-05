package com.github.dimitryivaniuta.videometadata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point for the Video Metadata Service Application.
 * <p>
 * Enables asynchronous method execution and caching, and bootstraps the Spring Boot context.
 * </p>
 */
@Slf4j
@SpringBootApplication
@EnableCaching
// @ConfigurationPropertiesScan("com.github.dimitryivaniuta.videometadata.config")
public class VideoMetadataServiceApplication {

    /**
     * Main method to bootstrap and run the Spring Boot application.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(VideoMetadataServiceApplication.class, args);
    }
}
