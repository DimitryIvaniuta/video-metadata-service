package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.model.Role;
import com.github.dimitryivaniuta.videometadata.model.User;
import com.github.dimitryivaniuta.videometadata.model.UserRole;
import com.github.dimitryivaniuta.videometadata.repository.UserRepository;
import com.github.dimitryivaniuta.videometadata.repository.UserRoleRepository;
import com.github.dimitryivaniuta.videometadata.util.DateTimeUtil;
import com.github.dimitryivaniuta.videometadata.web.dto.UserConnection;
import com.github.dimitryivaniuta.videometadata.web.dto.UserResponse;
import com.github.dimitryivaniuta.videometadata.web.dto.graphql.types.UserSort;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private static final int DEFAULT_PAGE      = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE     = 100;

    private final UserRepository userRepo;
    private final UserRoleRepository roleRepo;

    @Override
    public Mono<UserConnection> fetchUsers(Integer page, Integer pageSize,
                                           String search, UserSort sortBy, Boolean sortDesc) {
        final int p = normalizePage(page);
        final int s = normalizePageSize(pageSize);
        final long offset = (long) (p - 1) * s;
        final boolean desc = Boolean.TRUE.equals(sortDesc);
        final UserSort sort = (sortBy == null ? UserSort.USERNAME : sortBy);

        String term = (search == null) ? "" : search.trim();

        Mono<Long> totalMono = StringUtils.isBlank(term)
                ? userRepo.count()
                : userRepo.countByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term);

        Flux<User> pageFlux = switch (sort) {
            case USERNAME      -> desc
                    ? userRepo.pageBySearchOrderByUsernameDesc(nullIfBlank(term), s, offset)
                    : userRepo.pageBySearchOrderByUsernameAsc(nullIfBlank(term),  s, offset);
            case CREATED_AT    -> desc
                    ? userRepo.pageBySearchOrderByCreatedAtDesc(nullIfBlank(term), s, offset)
                    : userRepo.pageBySearchOrderByCreatedAtAsc(nullIfBlank(term),  s, offset);
            case UPDATED_AT    -> desc
                    ? userRepo.pageBySearchOrderByUpdatedAtDesc(nullIfBlank(term), s, offset)
                    : userRepo.pageBySearchOrderByUpdatedAtAsc(nullIfBlank(term),  s, offset);
            case LAST_LOGIN_AT -> desc
                    ? userRepo.pageBySearchOrderByLastLoginAtDesc(nullIfBlank(term), s, offset)
                    : userRepo.pageBySearchOrderByLastLoginAtAsc(nullIfBlank(term),  s, offset);
        };

        return pageFlux
                .collectList()
                .flatMap(list -> {
                    List<Long> ids = list.stream().map(User::getId).toList();
                    return loadRoles(ids).map(rolesMap ->
                            list.stream()
                                    .map(u -> toDto(u, rolesMap.getOrDefault(u.getId(), Set.of(Role.USER))))
                                    .toList()
                    );
                })
                .zipWith(totalMono)
                .map(t -> UserConnection.builder()
                        .items(t.getT1())
                        .page(p)
                        .pageSize(s)
                        .total(t.getT2())
                        .build());
    }

    @Override
    public Mono<Long> countUsers(String search) {
        return StringUtils.isBlank(search)
                ? userRepo.count()
                : userRepo.countByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search);
    }

    private Mono<Map<Long, Set<Role>>> loadRoles(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Mono.just(Map.of());
        }
        return roleRepo.findAllByUserIdIn(userIds)
                .collectMultimap(UserRole::getUserId, UserRole::getRole)
                .map(mm -> {
                    Map<Long, Set<Role>> out = new HashMap<>();
                    mm.forEach((id, roles) -> out.put(id, new HashSet<>(roles)));
                    userIds.forEach(id -> out.putIfAbsent(id, Set.of(Role.USER)));
                    return out;
                });
    }

    private static UserResponse toDto(User u, Set<Role> roles) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .status(u.getStatus())
                .createdAt(DateTimeUtil.toOffset(u.getCreatedAt()))
                .updatedAt(DateTimeUtil.toOffset(u.getUpdatedAt()))
                .lastLoginAt(DateTimeUtil.toOffset(u.getLastLoginAt()))
                .roles(roles == null ? Set.of() : Set.copyOf(roles))
                .build();
    }

    private static int normalizePage(Integer page) {
        return (page == null || page < 1) ? DEFAULT_PAGE : page;
    }

    private static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) return DEFAULT_PAGE_SIZE;
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private static String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
