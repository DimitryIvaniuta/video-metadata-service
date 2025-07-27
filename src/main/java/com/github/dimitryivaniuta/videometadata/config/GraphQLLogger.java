package com.github.dimitryivaniuta.videometadata.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@Slf4j
public class GraphQLLogger
{
    @Bean
    ApplicationRunner logGraphQlPath(Environment env) {
        return args -> {
            String base = env.getProperty("spring.webflux.base-path", "");
            String path = env.getProperty("spring.graphql.http.path", "/graphql");
            log.info("GraphQL endpoint mapped to: base:{}, path:{}", base, path);
        };
    }
}
