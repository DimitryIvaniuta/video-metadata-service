package com.github.dimitryivaniuta.videometadata.service.pipeline;

import com.github.dimitryivaniuta.videometadata.web.dto.pipeline.Country;
import com.github.dimitryivaniuta.videometadata.web.dto.pipeline.CountryData;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import static com.github.dimitryivaniuta.videometadata.util.TextUtils.foldToAsciiLower;

/**
 * Read-only service. With a database, swap the source with a reactive repository.
 */
@Service
public class CountryService {

    /**
     * Search by substring (accent-insensitive) in name or code.
     * Reactive pipelines are cancellable: if the client aborts, the Flux is disposed.
     */
    public Flux<Country> search(String query, int limit) {
        final String q = foldToAsciiLower(query == null ? "" : query.trim());
        if (q.isEmpty()) {
            return Flux.empty();
        }
        return Flux.fromIterable(CountryData.COUNTRIES)
                .filter(c -> {
                    final String name = foldToAsciiLower(c.name());
                    final String code = c.code().toLowerCase();
                    return name.contains(q) || code.contains(q);
                })
                .take(Math.max(1, limit)); // guard against 0/negative
    }
}
