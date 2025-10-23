package com.github.dimitryivaniuta.videometadata.web.dto.fxrate;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveQuoteNode {
    /**
     * e.g. "EURUSD" or "USDPLN" as returned by provider
     */
    private String symbol;
    private BigDecimal rate;
}