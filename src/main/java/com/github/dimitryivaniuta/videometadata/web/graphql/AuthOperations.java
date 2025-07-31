package com.github.dimitryivaniuta.videometadata.web.graphql;

import com.github.dimitryivaniuta.videometadata.domain.command.LoginCommand;
import com.github.dimitryivaniuta.videometadata.web.dto.TokenResponse;
import com.github.dimitryivaniuta.videometadata.handler.LoginCommandHandler;
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

    private final LoginCommandHandler loginHandler;

    @GraphQLMutation("login")
    public Mono<TokenResponse> login(
            @GraphQLArgument("username") @NotBlank String username,
            @GraphQLArgument("password") @NotBlank String password) {

        return loginHandler.handle(new LoginCommand(username, password));
    }
}
