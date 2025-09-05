package com.github.dimitryivaniuta.videometadata.web.dto.pipeline;

import java.util.List;

/**
 * Static in-memory dataset. Replace with a repository when wiring a DB.
 */
public final class CountryData {

    private CountryData() {}

    public static final List<Country> COUNTRIES = List.of(
            new Country("PL", "Poland"),
            new Country("DE", "Germany"),
            new Country("FR", "France"),
            new Country("ES", "Spain"),
            new Country("IT", "Italy"),
            new Country("NL", "Netherlands"),
            new Country("BE", "Belgium"),
            new Country("SE", "Sweden"),
            new Country("NO", "Norway"),
            new Country("DK", "Denmark"),
            new Country("IE", "Ireland"),
            new Country("CZ", "Czechia"),
            new Country("SK", "Slovakia"),
            new Country("LT", "Lithuania"),
            new Country("LV", "Latvia"),
            new Country("EE", "Estonia"),
            new Country("AT", "Austria"),
            new Country("HU", "Hungary"),
            new Country("RO", "Romania"),
            new Country("BG", "Bulgaria")
    );
}