package com.github.dimitryivaniuta.videometadata.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserLite {
    Long id;
    String username;
}