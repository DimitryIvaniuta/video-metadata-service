package com.github.dimitryivaniuta.videometadata.web.controller;

import com.github.dimitryivaniuta.videometadata.service.UserService;
import com.github.dimitryivaniuta.videometadata.web.dto.CreateUserRequest;
import com.github.dimitryivaniuta.videometadata.web.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
@Validated
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Create a new user.
     */
    @PostMapping
    public Mono<ResponseEntity<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        return userService.createUser(request)
                .map(dto -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(dto)
                );
    }

    /**
     * Get user by username.
     */
    @GetMapping("/username/{username}")
    public Mono<ResponseEntity<UserResponse>> getByUsername(
            @PathVariable String username) {

        return userService.findByUsername(username)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<UserResponse>> getById(
            @PathVariable Long id
    ) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}