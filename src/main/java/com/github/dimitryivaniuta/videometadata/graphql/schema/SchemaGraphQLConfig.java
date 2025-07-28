package com.github.dimitryivaniuta.videometadata.graphql.schema;

import graphql.scalars.ExtendedScalars;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Profile("schema-print")
@Configuration
public class SchemaGraphQLConfig {

    @Bean
    public RuntimeWiringConfigurer schemaRuntimeWiring() {
        return wiring -> wiring
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.GraphQLLong);
    }
}
