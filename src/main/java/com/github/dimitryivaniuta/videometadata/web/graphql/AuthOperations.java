package com.github.dimitryivaniuta.videometadata.web.graphql;

import com.github.dimitryivaniuta.videometadata.service.AuthService;
import com.github.dimitryivaniuta.videometadata.web.dto.TokenResponse;
import com.github.dimitryivaniuta.videometadata.graphql.annotations.*;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * GraphQL mutations for authentication.
 */
@Component
@GraphQLApplication
@RequiredArgsConstructor
public class AuthOperations {

    private final AuthService authService;

    @GraphQLMutation("authlogin")
    public Mono<TokenResponse> authlogin(
            @GraphQLArgument("username") @NotBlank String username,
            @GraphQLArgument("password") @NotBlank String password) {
        return authService.login(username, password);
    }
}