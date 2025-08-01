package com.github.dimitryivaniuta.videometadata.web.dto;

import com.github.dimitryivaniuta.videometadata.model.Role;
import com.github.dimitryivaniuta.videometadata.model.UserStatus;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.util.Set;

@Builder
public record UpdateUserInput(
        @NotNull                           Long id,
        @Size(min = 3, max = 50)           String username,
        @Email                             String email,
        UserStatus                         status,
        Set<Role>                          roles
) {}