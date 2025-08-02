package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.model.Role;
import com.github.dimitryivaniuta.videometadata.web.dto.CreateUserInput;
import com.github.dimitryivaniuta.videometadata.web.dto.CreateUserRequest;
import com.github.dimitryivaniuta.videometadata.web.dto.UpdateUserInput;
import com.github.dimitryivaniuta.videometadata.web.dto.UserResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Reactive user management operations.
 */
public interface UserService {

    /**
     * Create a new user (granted ROLE_USER by default).
     */
    Mono<UserResponse> createUser(CreateUserRequest request);

    /**
     * Lookup a user by username.
     */
    Mono<UserResponse> findByUsername(String username);

    /**
     * Update the lastLoginAt timestamp for the given user ID.
     */
    Mono<Void> updateLastLoginAt(Long userId);

    /**
     * Fetch a user by its numeric ID.
     */
    Mono<UserResponse> findById(Long id);

    Flux<UserResponse> list(int page, int size);

    Mono<UserResponse> createUser(CreateUserInput input);

    Mono<UserResponse> updateUser(UpdateUserInput input);

    Mono<Void> deleteUser(Long id);

    /**
     * Batch-load roles for a set of user IDs.
     * @return Map from userId -> set of roles
     */
    Mono<Map<Long, Set<Role>>> loadRolesByUserIds(Collection<Long> userIds);
}