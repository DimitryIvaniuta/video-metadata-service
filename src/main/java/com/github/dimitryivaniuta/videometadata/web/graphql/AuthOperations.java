package com.github.dimitryivaniuta.videometadata.web.graphql;

import com.github.dimitryivaniuta.videometadata.graphql.annotations.*;
import com.github.dimitryivaniuta.videometadata.service.AuthService;
import com.github.dimitryivaniuta.videometadata.web.dto.TokenResponse;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Public mutations for authentication.
 * <p>
 * - {@code login}   -> issues access + refresh (cookie)
 * - {@code refresh} -> rotates access token when called from the FE refresh-link
 */
@Component
@GraphQLApplication
@RequiredArgsConstructor
public class AuthOperations {

    private final AuthService authService;

    /* ----------- LOGIN (public) ----------------------------------------- */
    @GraphQLMutation("login")
    public Mono<TokenResponse> login(
            @GraphQLArgument("username") @NotBlank String username,
            @GraphQLArgument("password") @NotBlank String password,
            DataFetchingEnvironment env) {

        // Spring GraphQL puts ServerWebExchange in the GraphQlContext
        ServerWebExchange exchange =
                env.getGraphQlContext().get(ServerWebExchange.class);

        if (exchange == null) {
            return Mono.error(new IllegalStateException(
                    "ServerWebExchange not present in GraphQlContext"));
        }

        ServerHttpResponse response = exchange.getResponse(); // ← compiles

        return authService.login(username, password, response);
    }

    /* ----------- REFRESH (public) --------------------------------------- */
    @GraphQLMutation("refresh")
    public Mono<TokenResponse> refresh(
            @ContextValue ServerHttpRequest  request,                         // <──
            @ContextValue ServerHttpResponse response
    ) {
        return authService.refresh(request, response);
    }
}
