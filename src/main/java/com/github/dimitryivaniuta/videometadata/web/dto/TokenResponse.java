package com.github.dimitryivaniuta.videometadata.web.dto;

import lombok.Builder;

@Builder
public record TokenResponse(String token,
                            long expiresAt) {

}