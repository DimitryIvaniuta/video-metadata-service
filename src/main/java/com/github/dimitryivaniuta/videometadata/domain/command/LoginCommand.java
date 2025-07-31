package com.github.dimitryivaniuta.videometadata.domain.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * Command representing a login attempt.
 */
@RequiredArgsConstructor
@Getter
public class LoginCommand {
    private final String username;
    private final String password;

    /**
     * Convert to Spring’s Authentication token.
     */
    public UsernamePasswordAuthenticationToken toAuthToken() {
        return new UsernamePasswordAuthenticationToken(username, password);
    }
}
