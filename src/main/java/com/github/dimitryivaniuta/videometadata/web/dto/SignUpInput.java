package com.github.dimitryivaniuta.videometadata.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record SignUpInput(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 6, max = 72)  String password,
        @NotBlank @Email                     String email
) {
    /** Roles are fixed -> USER */
    public Set<com.github.dimitryivaniuta.videometadata.model.Role> initialRoles() {
        return Set.of(com.github.dimitryivaniuta.videometadata.model.Role.USER);
    }
}