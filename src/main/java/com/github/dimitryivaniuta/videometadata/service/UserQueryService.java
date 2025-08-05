package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.web.dto.UserConnection;
import com.github.dimitryivaniuta.videometadata.web.dto.graphql.types.UserSort;
import reactor.core.publisher.Mono;

public interface UserQueryService {

    Mono<UserConnection> fetchUsers(Integer page, Integer pageSize, String search,
                                    UserSort sortBy, Boolean sortDesc);

    Mono<Long> countUsers(String search);
}
