package com.github.dimitryivaniuta.videometadata.web.controller;

import com.github.dimitryivaniuta.videometadata.service.pipeline.CountryService;
import com.github.dimitryivaniuta.videometadata.web.dto.pipeline.Country;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * GET /api/countries?query=po&limit=20
 *
 * Designed for fetch(..., { signal }) on the client. When the browser aborts,
 * Spring WebFlux cancels the response publisher automatically.
 */
@Validated
@RestController
@RequestMapping(path = "/api/countries", produces = MediaType.APPLICATION_JSON_VALUE)
public class CountryController {

    private final CountryService service;

    public CountryController(CountryService service) {
        this.service = service;
    }

    @GetMapping
    public Flux<Country> search(
            @RequestParam(name = "query")
            @Size(min = 1, max = 64, message = "query must be 1..64 characters")
            String query,
            @RequestParam(name = "limit", defaultValue = "20")
            @Min(1) @Max(100)
            int limit
    ) {
        return service.search(query, limit);
    }
}
