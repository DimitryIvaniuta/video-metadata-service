package com.github.dimitryivaniuta.videometadata.web.graphql;

import com.github.dimitryivaniuta.videometadata.graphql.annotations.GraphQLApplication;
import com.github.dimitryivaniuta.videometadata.graphql.annotations.GraphQLArgument;
import com.github.dimitryivaniuta.videometadata.graphql.annotations.GraphQLField;
import com.github.dimitryivaniuta.videometadata.graphql.schema.RequiresRole;
import com.github.dimitryivaniuta.videometadata.service.RatesQueryService;
import com.github.dimitryivaniuta.videometadata.service.UserQueryService;
import com.github.dimitryivaniuta.videometadata.service.UserService;
import com.github.dimitryivaniuta.videometadata.service.VideoQueryService;
import com.github.dimitryivaniuta.videometadata.web.dto.UserConnection;
import com.github.dimitryivaniuta.videometadata.web.dto.UserResponse;
import com.github.dimitryivaniuta.videometadata.web.dto.VideoConnection;
import com.github.dimitryivaniuta.videometadata.web.dto.fxrate.FxRatesPayload;
import com.github.dimitryivaniuta.videometadata.web.dto.graphql.types.UserSort;
import com.github.dimitryivaniuta.videometadata.web.dto.graphql.types.VideoSort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@GraphQLApplication
@RequiredArgsConstructor
public class QueryOperations {

    private final UserService userService;       // for `me`
    private final VideoQueryService videoQueryService; // paging/filter/sort in service
    private final UserQueryService userQueryService;  // paging/filter/sort in service
    private final RatesQueryService ratesQueryService;

    /* ───────────────────────────── me ─────────────────────────── */
    @GraphQLField("me")
    public Mono<UserResponse> me() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .onErrorResume(e -> Mono.empty())
                .flatMap(userService::findByUsername)   // returns Mono<UserResponse>
                .switchIfEmpty(Mono.empty());
    }

    /* ─────────────────────────── videos ───────────────────────── */
    @GraphQLField("connectionVideos")
    public Mono<VideoConnection> videos(
            @GraphQLArgument("page") Integer page,
            @GraphQLArgument("pageSize") Integer pageSize,
            @GraphQLArgument("provider") String provider,
            @GraphQLArgument("sortBy") VideoSort sortBy,
            @GraphQLArgument("sortDesc") Boolean sortDesc
    ) {
        return videoQueryService.fetchVideos(page, pageSize, provider, sortBy, sortDesc);
    }

    @GraphQLField("connectionVideosCount")
    public Mono<Long> videosCount(
            @GraphQLArgument("provider") String provider
    ) {
        return videoQueryService.countVideos(provider);
    }

    @GraphQLField("connectionUsers")
    @RequiresRole({"ADMIN"})
    public Mono<UserConnection> users(
            @GraphQLArgument("page")     Integer page,
            @GraphQLArgument("pageSize") Integer pageSize,
            @GraphQLArgument("search")   String search,
            @GraphQLArgument("sortBy")   UserSort sortBy,
            @GraphQLArgument("sortDesc") Boolean sortDesc
    ) {
        return userQueryService.fetchUsers(page, pageSize, search, sortBy, sortDesc);
    }

    @GraphQLField("connectionUsersCount")
    @RequiresRole({"ADMIN"})
    public Mono<Long> usersCount(@GraphQLArgument("search") String search) {
        return userQueryService.countUsers(search);
    }


    @GraphQLField("fxRates")
    public Mono<FxRatesPayload> fxRates(
            @GraphQLArgument("base") String base,
            @GraphQLArgument("symbols") java.util.List<String> symbols
    ) {
        return ratesQueryService.fetchRates(base, symbols);
    }

}
