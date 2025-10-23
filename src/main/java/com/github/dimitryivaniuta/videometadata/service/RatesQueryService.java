package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.web.dto.fxrate.ConvertPayload;
import com.github.dimitryivaniuta.videometadata.web.dto.fxrate.FxRatesPayload;
import com.github.dimitryivaniuta.videometadata.web.dto.fxrate.LiveRatesPayload;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RatesQueryService {
    Mono<FxRatesPayload> fetchRates(String base, List<String> symbols);
    Mono<LiveRatesPayload> live(String source, java.util.List<String> currencies);
    Mono<ConvertPayload> convert(String from, String to, java.math.BigDecimal amount, String date);
}
