package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.web.dto.UserConnection;
import com.github.dimitryivaniuta.videometadata.web.dto.UserLite;
import com.github.dimitryivaniuta.videometadata.web.dto.graphql.types.UserSort;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UserQueryService {

    Mono<UserConnection> fetchUsers(Integer page, Integer pageSize, String search,
                                    UserSort sortBy, Boolean sortDesc);

    Mono<Long> countUsers(String search);

    Mono<List<UserLite>> searchUsersByUsername(String term, Integer limit);

}
