package com.github.dimitryivaniuta.videometadata.graphql.schema;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs on startup (schema-print profile) and writes the executable SDL to a file.
 */
@Component
@Profile("schema-print")
@Slf4j
public class SchemaExporter implements ApplicationRunner {

    private final GraphQlSource graphQlSource;

    @Value("${app.graphql.schema.output}")
    private String outputPath;

    public SchemaExporter(GraphQlSource graphQlSource) {
        this.graphQlSource = graphQlSource;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) throws Exception {
        GraphQLSchema schema = graphQlSource.schema();

        SchemaPrinter.Options options = SchemaPrinter.Options.defaultOptions()
                .includeScalarTypes(true)
                .includeSchemaDefinition(true)
                .includeDirectives(true)
                .includeIntrospectionTypes(false);

        String sdl = new SchemaPrinter(options).print(schema);

        Path path = Path.of(outputPath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, sdl, StandardCharsets.UTF_8);

        log.info("GraphQL schema written to {}", path.toAbsolutePath());
    }
}
