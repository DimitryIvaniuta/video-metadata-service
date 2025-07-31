package com.github.dimitryivaniuta.videometadata.web.controller;

import com.github.dimitryivaniuta.videometadata.model.VideoProvider;
import com.github.dimitryivaniuta.videometadata.service.*;
import com.github.dimitryivaniuta.videometadata.web.dto.*;
import com.github.dimitryivaniuta.videometadata.graphql.exceptions.GraphQlServiceException;
import com.github.dimitryivaniuta.videometadata.web.dto.imports.VideoResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class GraphqlController {

    private final ReactiveAuthenticationManager authManager;
    private final JwtTokenProvider            tokenProvider;
    private final UserService                 userService;
    private final VideoService                videoService;
    private final UserCacheService            userCacheService;

    // Login Mutation
    @MutationMapping("userlogin")
    public Mono<TokenResponse> login(
            @Argument @NotBlank String username,
            @Argument @NotBlank String password
    ) {
        UsernamePasswordAuthenticationToken creds =
                new UsernamePasswordAuthenticationToken(username, password);

        return authManager.authenticate(creds)
                .flatMap(this::issueAndPrimeCache)
                .onErrorMap(AuthenticationException.class,
                        ex -> new GraphQlServiceException("Invalid username or password", ex)
                );
    }

    private Mono<TokenResponse> issueAndPrimeCache(Authentication auth) {
        // 1) generate JWT
        Mono<TokenResponse> tokenMono = tokenProvider.generateToken(auth);

        // 2) update lastLoginAt + prime Redis cache
        Mono<Void> prime = userService.findByUsername(auth.getName())
                .switchIfEmpty(Mono.error(new GraphQlServiceException("User not found")))
                .flatMap(u -> userService.updateLastLoginAt(u.id()).thenReturn(u))
                .flatMap(u -> userCacheService.getUser(u.username()).then())
                .then();

        // 3) sequence both
        return tokenMono.flatMap(tr -> prime.thenReturn(tr));
    }

    // Get User Query (ADMIN only)
    @QueryMapping("user")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<UserResponse> userById(@Argument Long id) {
        return userService.findById(id)
                .switchIfEmpty(Mono.error(new GraphQlServiceException("No user with id=" + id)));
    }

    // Import Video Mutation
    @MutationMapping("importVideo")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Mono<VideoResponse> importVideo(
            @Argument @NotBlank VideoProvider provider,
            @Argument("externalVideoId") @NotBlank String externalId
    ) {
        return videoService.importVideo(provider, externalId)
                .onErrorMap(ex -> new GraphQlServiceException(
                        "Failed to import video from " + provider + "/" + externalId, ex
                ));
    }
}
