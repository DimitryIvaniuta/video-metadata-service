package com.github.dimitryivaniuta.videometadata.repository;

import com.github.dimitryivaniuta.videometadata.model.User;
import reactor.core.publisher.Flux;

import java.util.Collection;

public interface UserRepositoryCustom {
    Flux<User> findAllByIds(Collection<Long> ids);
}