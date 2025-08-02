package com.github.dimitryivaniuta.videometadata.web.graphql;

import com.github.dimitryivaniuta.videometadata.graphql.annotations.*;
import com.github.dimitryivaniuta.videometadata.graphql.schema.RequiresRole;
import com.github.dimitryivaniuta.videometadata.service.*;
import com.github.dimitryivaniuta.videometadata.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import reactor.core.publisher.*;

/**
 * GraphQL queries and mutations related to users and auth.
 */
@Component
@GraphQLApplication
@RequiredArgsConstructor
public class UsersOperations {

    private final UserService userService;

//            @ContextValue(DataLoaderConfig.USER_ROLES_LOADER)
//            DataLoader<Long, Set<Role>> rolesLoader
    @GraphQLField("user")
    @RequiresRole({"ADMIN"})
    public Mono<UserResponse> userById(
            @GraphQLArgument("id") @Min(1) long id
    ) {
        return userService.findById(id);
    }

    @GraphQLField("users")
    @RequiresRole({"ADMIN"})
    public Flux<UserResponse> listUsers(
            @GraphQLArgument("page") @Min(0) int page,
            @GraphQLArgument("size") @Min(1) int size) {
        return userService.list(page, size);
    }

    @GraphQLMutation("createUser")
    @RequiresRole({"ADMIN"})
    public Mono<UserResponse> createUser(
            @GraphQLArgument("input") @Valid CreateUserInput input) {
        return userService.createUser(input);
    }

    @GraphQLMutation("updateUser")
    @RequiresRole({"ADMIN"})
    public Mono<UserResponse> updateUser(
            @GraphQLArgument("input") @Valid UpdateUserInput input) {
        return userService.updateUser(input);
    }

    @GraphQLMutation("deleteUser")
    @RequiresRole({"ADMIN"})
    public Mono<Boolean> deleteUser(
            @GraphQLArgument("id") @Min(1) long id) {
        return userService.deleteUser(id).thenReturn(true);
    }

}
