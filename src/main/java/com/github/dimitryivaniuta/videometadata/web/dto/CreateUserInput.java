package com.github.dimitryivaniuta.videometadata.web.dto;

import com.github.dimitryivaniuta.videometadata.model.Role;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.util.Set;

@Builder
public record CreateUserInput(
        @NotBlank @Size(min = 3, max = 50)  String username,
        @NotBlank @Size(min = 8)            String password,
        @Email @NotBlank                    String email,
        @NotEmpty                           Set<Role> roles
) {}