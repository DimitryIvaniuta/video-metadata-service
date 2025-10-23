package com.github.dimitryivaniuta.videometadata.web.dto.fxrate;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveRatesPayload {
    /**
     * provider “source” (base) currency, e.g. "EUR"
     */
    private String source;
    private List<LiveQuoteNode> quotes;
}