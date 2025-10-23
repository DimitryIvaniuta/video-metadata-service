package com.github.dimitryivaniuta.videometadata.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class FxWebClientConfig {

    private final FxProps fxProps;

    @Bean
    public WebClient fxWebClient() {
        var http = HttpClient.create().responseTimeout(Duration.ofMillis(fxProps.provider().timeoutMs()));
        return WebClient.builder()
                .baseUrl(fxProps.base())
                .clientConnector(new ReactorClientHttpConnector(http))
                .build();
    }
}