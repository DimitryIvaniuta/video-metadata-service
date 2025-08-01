package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.web.dto.CreateUserInput;
import com.github.dimitryivaniuta.videometadata.web.dto.CreateUserRequest;
import com.github.dimitryivaniuta.videometadata.web.dto.UpdateUserInput;
import com.github.dimitryivaniuta.videometadata.web.dto.UserResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

}