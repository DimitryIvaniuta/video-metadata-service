package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.config.FxProps;
import com.github.dimitryivaniuta.videometadata.web.dto.fxrate.ExHostDto;
import com.github.dimitryivaniuta.videometadata.web.dto.fxrate.FxRateNode;
import com.github.dimitryivaniuta.videometadata.web.dto.fxrate.FxRatesPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RatesQueryServiceImpl implements RatesQueryService {

    private final WebClient fxWebClient;
    private final FxProps fxProps;

    // USD-based pivot (USD per 1 unit of currency)
    private static final Map<String, Double> USD_PIVOT = Map.of(
            "USD", 1.00,
            "EUR", 1.08,
            "CAD", 0.74,
            "AUD", 0.66,
            "NZD", 0.61,
            "INR", 0.012
    );

    @Override
    public Mono<FxRatesPayload> fetchRates(String base, List<String> symbols) {
        final String b = (base == null || base.isBlank()) ? fxProps.base() : base.trim();
        final List<String> requested = (symbols == null || symbols.isEmpty())
                ? Arrays.stream(fxProps.symbols().split(",")).map(String::trim).toList()
                : symbols.stream().map(String::trim).toList();

        return fxWebClient.get()
                .uri(u -> u.path("/live")
                        .queryParam("access_key", fxProps.apiKey())
                        .queryParam("base", b)
                        .queryParam("symbols", String.join(",", requested))
                        .build())
                .retrieve()
                .bodyToMono(ExHostDto.class) // <-- typed
                .doOnNext(json -> log.debug("FX raw response: {}", json))
                .map(dto -> {
                    var rates = dto.rates(); // Map<String, Double>
                    if (rates == null || rates.isEmpty()) {
                        throw new IllegalStateException("empty rates");
                    }
                    var list = requested.stream()
                            .map(sym -> FxRateNode.builder()
                                    .currency(sym)
                                    .rate(BigDecimal.valueOf(
                                            rates.getOrDefault(sym, 0.0d)))
                                    .build())
                            .toList();
                    return FxRatesPayload.builder().base(b).rates(list).build();
                })
                .onErrorResume(e -> Mono.just(FxRatesPayload.builder()
                        .base(b)
                        .rates(defaultRatesForBase(b, requested))
                        .build()));
    }

    // Convert USD_PIVOT (USD per currency) into rates for requested base:
// rate(base->sym) = (USD per base) / (USD per sym)
    private static List<FxRateNode> defaultRatesForBase(String base, List<String> requested) {
        double usdPerBase = USD_PIVOT.getOrDefault(base, Double.NaN);
        return requested.stream().map(sym -> {
            double usdPerSym = USD_PIVOT.getOrDefault(sym, Double.NaN);
            double v = (Double.isFinite(usdPerBase) && Double.isFinite(usdPerSym))
                    ? usdPerBase / usdPerSym     // e.g., base=EUR → rate(USD) ≈ 1.09
                    : 0.0;
            return FxRateNode.builder().currency(sym).rate(BigDecimal.valueOf(v)).build();
        }).toList();
    }
}
