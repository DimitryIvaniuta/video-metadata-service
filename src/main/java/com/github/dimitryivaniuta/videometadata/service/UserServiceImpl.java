package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.model.Role;
import com.github.dimitryivaniuta.videometadata.model.User;
import com.github.dimitryivaniuta.videometadata.model.UserRole;
import com.github.dimitryivaniuta.videometadata.model.UserStatus;
import com.github.dimitryivaniuta.videometadata.repository.UserRepository;
import com.github.dimitryivaniuta.videometadata.repository.UserRoleRepository;
import com.github.dimitryivaniuta.videometadata.util.DateTimeUtil;
import com.github.dimitryivaniuta.videometadata.web.dto.CreateUserInput;
import com.github.dimitryivaniuta.videometadata.web.dto.CreateUserRequest;
import com.github.dimitryivaniuta.videometadata.web.dto.UpdateUserInput;
import com.github.dimitryivaniuta.videometadata.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.dimitryivaniuta.videometadata.web.dto.UserResponse.toDto;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final UserRoleRepository roleRepo;
    private final PasswordEncoder     passwordEncoder;

    @Override
    public Mono<UserResponse> createUser(CreateUserRequest req) {
        // build and save the User entity
        User user = User.builder()
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .email(req.email())
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return userRepo.save(user)
                .flatMap(saved -> {
                    // grant default USER role
                    UserRole ur = UserRole.builder()
                            .userId(saved.getId())
                            .role(Role.USER)
                            .build();
                    return roleRepo.save(ur)
                            .thenReturn(saved);
                })
                .flatMap(this::mapToResponse);
    }

    public Mono<UserResponse> findByUsernameWithRoles(String username) {
        return userRepo.findByUsername(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException(username)))
                // load roles for that user
                .flatMap(user ->
                        roleRepo.findAllByUserId(user.getId())
                                .map(UserRole::getRole)       // Role enum
                                .collect(Collectors.toSet())
                        .map(roles -> toDto(user, roles))
                );
    }

    @Override
    public Mono<UserResponse> findByUsername(String username) {
        return userRepo.findByUsername(username)
                .switchIfEmpty(
                        Mono.error(new UsernameNotFoundException(username))
                )
                .flatMap(this::joinUserWithRoles);
    }

    @Override
    public Mono<Void> updateLastLoginAt(Long userId) {
        return userRepo.findById(userId)
                .flatMap(u -> {
                    u.setLastLoginAt(Instant.now());
                    u.setUpdatedAt(Instant.now());
                    return userRepo.save(u);
                })
                .then();
    }

    @Override
    public Flux<UserResponse> list(int page, int size) {
        return userRepo.findAll()
                .skip((long) page * size)
                .take(size)
                .flatMap(this::joinUserWithRoles);
    }


    @Override
    public Mono<UserResponse> createUser(CreateUserInput in) {
        User u = User.builder()
                .username(in.username())
                .password(passwordEncoder.encode(in.password()))
                .email(in.email())
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return userRepo.save(u)
                .flatMap(saved ->
                        roleRepo.saveAll(
                                        in.roles().stream()
                                                .map(r -> new UserRole(null, saved.getId(), r))
                                                .toList())
                                .then(Mono.just(saved))
                )
                .flatMap(this::joinUserWithRoles)
                .onErrorMap(DuplicateKeyException.class,
                        ex -> new IllegalStateException("Username or email already exists", ex));
    }

    @Override
    public Mono<UserResponse> updateUser(UpdateUserInput in) {
        return userRepo.findById(in.id())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found")))
                .flatMap(u -> {
                    if (in.username() != null) u.setUsername(in.username());
                    if (in.email()    != null) u.setEmail(in.email());
                    if (in.status()   != null) u.setStatus(in.status());
                    u.setUpdatedAt(Instant.now());
                    return userRepo.save(u);
                })
                .flatMap(saved -> {
                    if (in.roles() == null) return Mono.just(saved);
                    // Replace roles atomically
                    return roleRepo.deleteAllByUserId(saved.getId()).then(
                            roleRepo.saveAll(
                                            in.roles().stream()
                                                    .map(r -> new UserRole(null, saved.getId(), r))
                                                    .toList())
                                    .then(Mono.just(saved))
                    );
                })
                .flatMap(this::joinUserWithRoles);
    }

    @Override
    public Mono<Void> deleteUser(Long id) {
        return roleRepo.deleteAllByUserId(id)
                .then(userRepo.deleteById(id));
    }

    /**
     * Fetch roles and assemble the UserResponse DTO.
     */
    private Mono<UserResponse> mapToResponse(User user) {
        return roleRepo.findAllByUserId(user.getId())
                .map(UserRole::getRole)
                .collectList()
                .map(list ->
                        UserResponse.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .status(user.getStatus())
                                .createdAt(DateTimeUtil.toOffset(user.getCreatedAt()))
                                .updatedAt(DateTimeUtil.toOffset(user.getUpdatedAt()))
                                .roles(Set.copyOf(list))
                                .build()
                );
    }

    public Mono<UserResponse> findById(Long userId) {
        return userRepo.findById(userId)
                .flatMap(this::joinUserWithRoles);
    }


    private Mono<UserResponse> joinUserWithRoles(User user) {
        return roleRepo.findAllByUserId(user.getId())
                .map(UserRole::getRole)
                .collect(Collectors.toSet())
                .map(roles -> toDto(user, roles));
    }
}
