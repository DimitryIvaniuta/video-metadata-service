package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.model.UserRole;
import com.github.dimitryivaniuta.videometadata.model.UserStatus;
import com.github.dimitryivaniuta.videometadata.repository.UserRepository;
import com.github.dimitryivaniuta.videometadata.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements ReactiveUserDetailsService {

    private final UserRepository userRepo;
    private final UserRoleRepository roleRepo;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepo.findByUsername(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found: " + username)))
                .flatMap(user -> {
                    if (user.getStatus() != UserStatus.ACTIVE) {
                        return Mono.error(new DisabledException("User is not active: " + username));
                    }
                    return roleRepo.findByUserId(user.getId())
                            .map(UserRole::getRole)
                            .map(r -> "ROLE_" + r)
                            .collectList()
                            .flatMap(roleNames -> {
                                // Log all roles loaded for this user
                                log.debug("Loaded roles for user [{}]: {}", username, roleNames);

                                var authorities = roleNames.stream()
                                        .map(SimpleGrantedAuthority::new)
                                        .toList();

                                UserDetails userDetails = User.withUsername(user.getUsername())
                                        .password(user.getPassword())
                                        .authorities(authorities)
                                        .build();

                                return Mono.just(userDetails);
                            });
                });
    }
}
