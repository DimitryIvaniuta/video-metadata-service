package com.github.dimitryivaniuta.videometadata.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class FxWebClientConfig {

    @Bean(name = "fxWebClient")
    public WebClient fxWebClient(FxProps props) {
        String url = props.provider().url();               // must be like "https://api.exchangerate.host"
        if (url == null || url.isBlank() || url.equalsIgnoreCase("USD")) {
            // harden against misconfiguration
            url = "https://api.exchangerate.host";
        } else if (!url.startsWith("http")) {
            url = "https://" + url;
        }

        HttpClient http = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5));

        return WebClient.builder()
                .baseUrl(url)
                .clientConnector(new ReactorClientHttpConnector(http))
                // log every request/response status
                .filters(filters -> {
                    filters.add(ExchangeFilterFunction.ofRequestProcessor(req -> {
                        System.out.println("[FX] REQUEST " + req.method() + " " + req.url());
                        return Mono.just(req);
                    }));
                    filters.add(ExchangeFilterFunction.ofResponseProcessor(res -> {
                        System.out.println("[FX] RESPONSE " + res.statusCode());
                        return Mono.just(res);
                    }));
                })
                .build();
    }
}