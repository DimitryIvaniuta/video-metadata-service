package com.github.dimitryivaniuta.videometadata.graphql.schema;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@SpringBootApplication(
        scanBasePackages = "com.github.dimitryivaniuta.videometadata"
)
@Slf4j
public class SchemaPrinterApplication {
    public static void main(String[] args) {

        log.info("Starting SchemaPrinterApplication");
//        SpringApplication.run(SchemaPrinterApplication.class, args);
        new SpringApplicationBuilder(SchemaPrinterApplication.class)
                .web(WebApplicationType.NONE)
                .listeners(e -> {
                    if (e instanceof ApplicationReadyEvent ev) {
                        SpringApplication.exit(ev.getApplicationContext(), () -> 0);
                    }
                })
                .run(args);
    }
}