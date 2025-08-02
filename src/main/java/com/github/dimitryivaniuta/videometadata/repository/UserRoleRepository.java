package com.github.dimitryivaniuta.videometadata.repository;

import com.github.dimitryivaniuta.videometadata.model.UserRole;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.Collection;

public interface UserRoleRepository extends ReactiveCrudRepository<UserRole, Long> {
    Flux<UserRole> findAllByUserId(Long userId);
    Flux<UserRole> deleteAllByUserId(Long userId);
    Flux<UserRole> findAllByUserIdIn(Collection<Long> userIds);
}