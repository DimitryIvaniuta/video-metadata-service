package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.config.FxProps;
import com.github.dimitryivaniuta.videometadata.web.dto.fxrate.FxRateNode;
import com.github.dimitryivaniuta.videometadata.web.dto.fxrate.FxRatesPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatesQueryServiceImpl implements RatesQueryService {

    private final WebClient fxWebClient;
    private final FxProps fxProps;

    @Override
    public Mono<FxRatesPayload> fetchRates(String base, List<String> symbols) {
        final String b = (base == null || base.isBlank()) ? fxProps.base() : base.trim();
        final List<String> requested = (symbols == null || symbols.isEmpty())
                ? Arrays.stream(fxProps.symbols().split(",")).map(String::trim).toList()
                : symbols.stream().map(String::trim).toList();

        return fxWebClient.get()
                .uri(uri -> uri
                        .path("/latest")
                        .queryParam("access_key", fxProps.apiKey())          // exchangerate.host requires key
                        .queryParam("base", b)
                        .queryParam("symbols", String.join(",", requested))
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .map(body -> {
                    Map<String, Object> rates = (Map<String, Object>) body.getOrDefault("rates", Map.of());
                    // normalize & preserve requested order; fill missing with 0
                    List<FxRateNode> list = requested.stream()
                            .map(sym -> FxRateNode.builder()
                                    .currency(sym)
                                    .rate(BigDecimal.valueOf(
                                            Optional.ofNullable(rates.get(sym))
                                                    .map(Number.class::cast)
                                                    .map(Number::doubleValue)
                                                    .orElse(0.0d)))
                                    .build())
                            .collect(Collectors.toList());
                    return FxRatesPayload.builder().base(b).rates(list).build();
                })
                .onErrorResume(e ->
                        // graceful fallback to deterministic defaults
                        Mono.just(FxRatesPayload.builder()
                                .base(b)
                                .rates(defaultRates(requested))
                                .build()));
    }

    private static List<FxRateNode> defaultRates(List<String> requested) {
        Map<String, Double> def = Map.of(
                "INR", 0.012, "CAD", 0.74, "EUR", 1.09, "AUD", 0.66, "NZD", 0.61
        );
        return requested.stream()
                .map(sym -> FxRateNode.builder()
                        .currency(sym)
                        .rate(BigDecimal.valueOf(def.getOrDefault(sym, 0.0)))
                        .build())
                .toList();
    }
}
