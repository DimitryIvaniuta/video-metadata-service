package com.github.dimitryivaniuta.videometadata.repository;

import com.github.dimitryivaniuta.videometadata.model.UserRole;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface UserRoleRepository extends ReactiveCrudRepository<UserRole, Long> {
    Flux<UserRole> findByUserId(Long userId);
}