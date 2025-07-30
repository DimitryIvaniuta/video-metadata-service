package com.github.dimitryivaniuta.videometadata.web.graphql;

import com.github.dimitryivaniuta.videometadata.graphql.annotations.*;
import com.github.dimitryivaniuta.videometadata.graphql.schema.RequiresRole;
import com.github.dimitryivaniuta.videometadata.service.*;
import com.github.dimitryivaniuta.videometadata.web.dto.*;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * GraphQL queries and mutations related to users and auth.
 */
@Component
@GraphQLApplication
@RequiredArgsConstructor
public class UsersOperations {

    private final ReactiveAuthenticationManager authManager;
    private final JwtTokenProvider              tokenProvider;
    private final UserService                   userService;
    private final UserCacheService              userCache;

    /* MUTATION: login                                                       */
    @GraphQLMutation("login")
    public Mono<TokenResponse> login(
            @GraphQLArgument("username") @NotBlank String username,
            @GraphQLArgument("password") @NotBlank String password) {

        var creds = new UsernamePasswordAuthenticationToken(username, password);

        return authManager.authenticate(creds)
                .onErrorMap(AuthenticationException.class,
                        ex -> new RuntimeException("Invalid credentials"))
                .flatMap(auth -> tokenProvider.generateToken(auth)
                        .flatMap(token ->
                                userService.findByUsername(username)
                                        .flatMap(u -> userService.updateLastLoginAt(u.id()).thenReturn(u))
                                        .flatMap(u -> userCache.getUser(u.username()).thenReturn(token))
                        )
                );
    }

    /* QUERY: user(id) - ADMIN only                                          */
    @GraphQLField("user")
    @RequiresRole("ADMIN")
    public Mono<UserResponse> userById(@GraphQLArgument("id") Long id) {
        return userService.findById(id);
    }
}
