package com.github.dimitryivaniuta.videometadata.repository;

import com.github.dimitryivaniuta.videometadata.model.User;
import com.github.dimitryivaniuta.videometadata.model.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
@ConditionalOnBean(DatabaseClient.class)
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final DatabaseClient db;

    @Override
    public Flux<User> findAllByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Flux.empty();
        }

        // Build dynamic IN ($1,$2,$3,...)
        List<Long> idList = new ArrayList<>(ids);

        StringBuilder sql = new StringBuilder("""
            SELECT id,
                   username,
                   password,
                   email,
                   status,
                   created_at,
                   updated_at,
                   last_login_at
            FROM users
            WHERE id IN (
        """);

        for (int i = 0; i < idList.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("$").append(i + 1);
        }
        sql.append(")");

        DatabaseClient.GenericExecuteSpec spec = db.sql(sql.toString());
        for (int i = 0; i < idList.size(); i++) {
            spec = spec.bind(i, idList.get(i));
        }

        return spec
                .map((row, meta) -> {
                    Long   id          = row.get("id", Long.class);
                    String username    = row.get("username", String.class);
                    String password    = row.get("password", String.class);
                    String email       = row.get("email", String.class);

                    // <-- THIS is the important change:
                    Number statusNum   = row.get("status", Number.class);
                    UserStatus status  = UserStatus.fromCode(statusNum);

                    Instant createdAt  = row.get("created_at", Instant.class);
                    Instant updatedAt  = row.get("updated_at", Instant.class);
                    Instant lastLogin  = row.get("last_login_at", Instant.class);

                    return User.builder()
                            .id(id)
                            .username(username)
                            .password(password)
                            .email(email)
                            .status(status)
                            .createdAt(createdAt)
                            .updatedAt(updatedAt)
                            .lastLoginAt(lastLogin)
                            .build();
                })
                .all();
    }
}
