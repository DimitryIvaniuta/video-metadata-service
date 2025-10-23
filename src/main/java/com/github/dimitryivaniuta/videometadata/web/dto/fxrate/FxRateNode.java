package com.github.dimitryivaniuta.videometadata.web.dto.fxrate;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FxRateNode {
    private String currency;
    private BigDecimal rate;
}