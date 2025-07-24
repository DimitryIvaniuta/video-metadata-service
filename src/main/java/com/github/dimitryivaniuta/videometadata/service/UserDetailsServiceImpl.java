package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.model.UserRole;
import com.github.dimitryivaniuta.videometadata.model.UserStatus;
import com.github.dimitryivaniuta.videometadata.repository.UserRepository;
import com.github.dimitryivaniuta.videometadata.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements ReactiveUserDetailsService {
    private final UserRepository userRepo;
    private final UserRoleRepository roleRepo;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepo.findByUsername(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException(username)))
                .flatMap(u -> {
                    if (u.getStatus() != UserStatus.ACTIVE) {
                        return Mono.error(new DisabledException("User not active"));
                    }
                    return roleRepo.findByUserId(u.getId())
                            .map(UserRole::getRole)
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                            .collectList()
                            .map(auths -> User.withUsername(u.getUsername())
                                    .password(u.getPassword())
                                    .authorities(auths)
                                    .build());
                });
    }
}
