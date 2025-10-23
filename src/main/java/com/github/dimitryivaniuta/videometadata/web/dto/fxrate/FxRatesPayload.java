package com.github.dimitryivaniuta.videometadata.web.dto.fxrate;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FxRatesPayload {
    private String base;
    private List<FxRateNode> rates;
}