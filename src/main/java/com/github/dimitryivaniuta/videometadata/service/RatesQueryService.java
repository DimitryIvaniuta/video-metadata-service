package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.web.dto.fxrate.FxRatesPayload;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RatesQueryService {
    Mono<FxRatesPayload> fetchRates(String base, List<String> symbols);
}
