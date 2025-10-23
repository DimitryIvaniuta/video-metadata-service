package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.config.FxProps;
import com.github.dimitryivaniuta.videometadata.web.dto.fxrate.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
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
            "EUR", 1.071,
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

        final String symCsv = String.join(",", requested);
        log.info("FX: requesting base={}, symbols={}", b, symCsv);

        return fxWebClient.get()
                .uri(u -> u.path("/latest")
                        .queryParam("access_key", fxProps.apiKey())
                        .queryParam("base", b)
                        .queryParam("symbols", symCsv)
                        .build())
                .exchangeToMono(resp -> handleResponse(resp, b, requested))
                .doOnSuccess(p -> log.info("FX: success base={}, items={}", p.getBase(), p.getRates().size()))
                .doOnError(e -> log.error("FX: fetch failed (base={}, symbols={})", b, symCsv, e))
                .onErrorResume(e -> {
                    log.warn("FX: falling back to defaults for base={} due to: {}", b, e.toString());
                    return Mono.just(FxRatesPayload.builder()
                            .base(b)
                            .rates(defaultRatesForBase(b, requested))
                            .build());
                });
    }

    /** LIVE: /live?access_key=...&source=EUR&currencies=USD,AUD,...&format=1 */

    /** LIVE via /latest (preferred): returns quotes as source+symbol, e.g. EURUSD */
    @Override
    public Mono<LiveRatesPayload> live(String source, List<String> currencies) {
        final String src = (source == null || source.isBlank()) ? fxProps.base() : source.trim();
        final List<String> list = (currencies == null || currencies.isEmpty())
                ? Arrays.stream(fxProps.symbols().split(",")).map(String::trim).toList()
                : currencies.stream().map(String::trim).toList();
        final String csv = String.join(",", list);

        log.info("FX LIVE: baseUrl={}, source={}, currencies={}",
                fxProps.provider().url(), src, csv);

        return fxWebClient.get()
                .uri(u -> u.path("/live")
                        .queryParam("access_key", fxProps.apiKey())
                        .queryParam("source", src)
                        .queryParam("currencies", csv)
                        .queryParam("format", 1)
                        .build())
                .exchangeToMono(resp -> {
                    HttpStatusCode sc = resp.statusCode();
                    if (sc.isError()) {
                        return resp.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("FX LIVE HTTP {} error. Body: {}", sc.value(), body);
                                    return Mono.error(new IllegalStateException("FX LIVE HTTP " + sc.value()));
                                });
                    }
                    return resp.bodyToMono(new ParameterizedTypeReference<Map<String,Object>>() {})
                            .flatMap(map -> {
                                Object ok = map.get("success");
                                if (ok instanceof Boolean b && !b) {
                                    log.error("FX LIVE provider error: {}", map.get("error"));
                                    return Mono.error(new IllegalStateException("FX LIVE provider error"));
                                }
                                @SuppressWarnings("unchecked")
                                Map<String,Object> quotes = (Map<String,Object>) map.get("quotes");
                                if (quotes == null || quotes.isEmpty()) {
                                    log.error("FX LIVE empty quotes; payload={}", map);
                                    return Mono.error(new IllegalStateException("empty quotes"));
                                }
                                List<LiveQuoteNode> q = new ArrayList<>(quotes.size());
                                quotes.forEach((k,v) -> q.add(LiveQuoteNode.builder()
                                        .symbol(k)
                                        .rate(BigDecimal.valueOf(((Number) v).doubleValue()))
                                        .build()));
                                return Mono.just(LiveRatesPayload.builder().source(src).quotes(q).build());
                            });
                })
                .doOnSuccess(p -> log.info("FX LIVE OK: source={}, quotes={}", p.getSource(), p.getQuotes().size()))
                .onErrorResume(e -> {
                    log.warn("FX LIVE FALLBACK (source={}): {}", src, e.toString());
                    double usdPerSrc = USD_PIVOT.getOrDefault(src, Double.NaN);
                    List<LiveQuoteNode> q = list.stream().map(sym -> {
                        double usdPerSym = USD_PIVOT.getOrDefault(sym, Double.NaN);
                        double r = (Double.isFinite(usdPerSrc) && Double.isFinite(usdPerSym)) ? usdPerSrc / usdPerSym : 0.0;
                        return LiveQuoteNode.builder().symbol(src + sym).rate(BigDecimal.valueOf(r)).build();
                    }).toList();
                    return Mono.just(LiveRatesPayload.builder().source(src).quotes(q).build());
                });
    }

    /** CONVERT: /convert?access_key=...&from=USD&to=EUR&amount=25&format=1[&date=YYYY-MM-DD] */
    @Override
    public Mono<ConvertPayload> convert(String from, String to, BigDecimal amount, String date) {
        final String f = (from == null || from.isBlank()) ? fxProps.base() : from.trim();
        final String t = (to   == null || to.isBlank())   ? "USD"          : to.trim();
        final BigDecimal a = (amount == null || amount.signum() < 0) ? BigDecimal.ONE : amount;

        log.info("FX CONVERT: baseUrl={}, from={}, to={}, amount={}, date={}",
                fxProps.provider().url(), f, t, a, date);

        return fxWebClient.get()
                .uri(u -> {
                    var b = u.path("/convert")
                            .queryParam("access_key", fxProps.apiKey())  // REQUIRED
                            .queryParam("from", f)
                            .queryParam("to", t)
                            .queryParam("amount", a)
                            .queryParam("format", 1);
                    if (date != null && !date.isBlank()) b.queryParam("date", date.trim());
                    return b.build();
                })
                .exchangeToMono(resp -> {
                    HttpStatusCode sc = resp.statusCode();
                    if (sc.isError()) {
                        return resp.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("FX CONVERT HTTP {} error. Body: {}", sc.value(), body);
                                    return Mono.error(new IllegalStateException("FX CONVERT HTTP " + sc.value()));
                                });
                    }
                    // Body is either success or { success:false, error:{...} } — check it.
                    return resp.bodyToMono(new ParameterizedTypeReference<Map<String,Object>>() {})
                            .flatMap(map -> {
                                Object ok = map.get("success");
                                if (ok instanceof Boolean b && !b) {
                                    log.error("FX CONVERT provider error: {}", map.get("error"));
                                    return Mono.error(new IllegalStateException("FX CONVERT provider error"));
                                }
                                @SuppressWarnings("unchecked")
                                Map<String,Object> query = (Map<String,Object>) map.getOrDefault("query", Map.of());
                                @SuppressWarnings("unchecked")
                                Map<String,Object> info  = (Map<String,Object>) map.getOrDefault("info",  Map.of());

                                String from0 = String.valueOf(query.getOrDefault("from", f));
                                String to0   = String.valueOf(query.getOrDefault("to", t));
                                BigDecimal amount0 = BigDecimal.valueOf(((Number) query.getOrDefault("amount", a)).doubleValue());
                                BigDecimal rate0   = BigDecimal.valueOf(((Number) info.getOrDefault("rate", 0d)).doubleValue());
                                BigDecimal result0 = BigDecimal.valueOf(((Number) map.getOrDefault("result", 0d)).doubleValue());
                                String date0 = String.valueOf(map.getOrDefault("date", ""));

                                return Mono.just(ConvertPayload.builder()
                                        .fromCurr(from0)
                                        .toCurr(to0)
                                        .amount(amount0)
                                        .rate(rate0)
                                        .result(result0)
                                        .date(date0)
                                        .build());
                            });
                })
                .doOnSuccess(p -> log.info("FX CONVERT OK: {} -> {} amount={} rate={} result={} date={}",
                        p.getFromCurr(), p.getToCurr(), p.getAmount(), p.getRate(), p.getResult(), p.getDate()))
                .onErrorResume(e -> {
                    log.warn("FX CONVERT FALLBACK: {} -> {} amount={} cause={}", f, t, a, e.toString());
                    // fallback via USD pivot: rate(f→t) = (USD/f) / (USD/t)
                    double usdPerFrom = USD_PIVOT.getOrDefault(f, Double.NaN);
                    double usdPerTo   = USD_PIVOT.getOrDefault(t, Double.NaN);
                    double r = (Double.isFinite(usdPerFrom) && Double.isFinite(usdPerTo)) ? (usdPerFrom / usdPerTo) : 0.0;
                    BigDecimal rate = BigDecimal.valueOf(r);
                    BigDecimal res  = rate.multiply(a);
                    return Mono.just(ConvertPayload.builder()
                            .fromCurr(f).toCurr(t).amount(a).rate(rate).result(res).date(null).build());
                });
    }

    private Mono<FxRatesPayload> handleResponse(ClientResponse resp, String base, List<String> requested) {
        HttpStatusCode status = resp.statusCode();      // <-- HttpStatusCode (Spring 6)
        if (status.isError()) {
            return resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body -> {
                        log.error("FX: HTTP {} error. Body: {}", status.value(), body);
                        return Mono.error(new IllegalStateException("FX HTTP " + status.value()));
                    });
        }

        return resp.bodyToMono(ExHostDto.class)
                .doOnNext(dto -> log.debug("FX: dto base={}, ratesCount={}",
                        dto.base(), dto.rates() == null ? 0 : dto.rates().size()))
                .map(dto -> {
                    Map<String, Double> rates = dto.rates();
                    if (rates == null || rates.isEmpty()) {
                        throw new IllegalStateException("empty rates");
                    }
                    var list = requested.stream()
                            .map(sym -> FxRateNode.builder()
                                    .currency(sym)
                                    .rate(BigDecimal.valueOf(rates.getOrDefault(sym, 0.0d)))
                                    .build())
                            .toList();
                    return FxRatesPayload.builder().base(base).rates(list).build();
                });
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
