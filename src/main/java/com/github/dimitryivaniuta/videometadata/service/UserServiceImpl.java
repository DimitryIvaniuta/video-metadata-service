package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.model.Role;
import com.github.dimitryivaniuta.videometadata.model.User;
import com.github.dimitryivaniuta.videometadata.model.UserRole;
import com.github.dimitryivaniuta.videometadata.model.UserStatus;
import com.github.dimitryivaniuta.videometadata.repository.UserRepository;
import com.github.dimitryivaniuta.videometadata.repository.UserRoleRepository;
import com.github.dimitryivaniuta.videometadata.web.dto.CreateUserRequest;
import com.github.dimitryivaniuta.videometadata.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;

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

    @Override
    public Mono<UserResponse> findByUsername(String username) {
        return userRepo.findByUsername(username)
                .flatMap(this::mapToResponse);
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

    /**
     * Fetch roles and assemble the UserResponse DTO.
     */
    private Mono<UserResponse> mapToResponse(User user) {
        return roleRepo.findByUserId(user.getId())
                .map(UserRole::getRole)
                .collectList()
                .map(list ->
                        UserResponse.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .status(user.getStatus())
                                .createdAt(user.getCreatedAt())
                                .updatedAt(user.getUpdatedAt())
                                .roles(Set.copyOf(list.stream().map(Enum::name).toList()))
                                .build()
                );
    }

    public Mono<UserResponse> findById(Long userId) {
        return userRepo.findById(userId)
                .flatMap(this::mapToResponse);
    }
}
