package com.github.dimitryivaniuta.videometadata.web.dto.fxrate;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConvertPayload {
    private String fromCurr;
    private String toCurr;
    private BigDecimal amount;       // input amount
    private BigDecimal rate;         // unit rate used
    private BigDecimal result;       // converted amount
    private String date;             // optional provider date
}