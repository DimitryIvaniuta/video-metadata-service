package com.github.dimitryivaniuta.videometadata.web.dto.fxrate;

import java.util.Map;

/** Mirrors exchangerate.host /latest response we use */
public record ExHostDto(String base, Map<String, Double> rates) {}