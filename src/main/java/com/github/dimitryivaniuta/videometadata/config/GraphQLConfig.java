package com.github.dimitryivaniuta.videometadata.config;

import graphql.scalars.ExtendedScalars;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
public class GraphQLConfig {

    /**
     * Registers the java.time.Instant scalar (ISO‑8601 DateTime).
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiring -> wiring
                // java.time.Instant <-> DateTime (ISO-8601)
                .scalar(ExtendedScalars.DateTime)
                // Java long <-> GraphQL Long
                .scalar(ExtendedScalars.GraphQLLong);
    }
}
