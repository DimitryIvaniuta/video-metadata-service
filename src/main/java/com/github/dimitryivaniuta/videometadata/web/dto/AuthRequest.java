package com.github.dimitryivaniuta.videometadata.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(@NotBlank String username,
                          @NotBlank String password) {
}
