package com.github.dimitryivaniuta.videometadata.graphql.schema;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot app used ONLY to print the GraphQL schema.
 * Placed in a dedicated package so it does NOT scan regular application beans.
 */
@SpringBootApplication(
        scanBasePackages = "com.github.dimitryivaniuta.videometadata.graphql.schema"
)
public class SchemaPrinterApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchemaPrinterApplication.class, args);
    }
}