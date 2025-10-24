package com.github.dimitryivaniuta.videometadata.repository;

import com.github.dimitryivaniuta.videometadata.model.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
//@RequiredArgsConstructor
public class TicketRepositoryImpl implements TicketRepositoryCustom {

    private final DatabaseClient db;

    public TicketRepositoryImpl(ObjectProvider<DatabaseClient> dbProvider) {
        this.db = dbProvider.getIfAvailable(); // null if R2DBC not configured
    }

    private static final String SQL = """
            SELECT COUNT(*) AS cnt
            FROM tickets
            WHERE (:q IS NULL OR LOWER(title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR  LOWER(description) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:status IS NULL OR status = :status)
              AND (:assigneeId IS NULL OR assignee_id = :assigneeId)
              AND (:reporterId IS NULL OR reporter_id = :reporterId)
            """;

    @Override
    public Mono<Long> countFiltered(String q, TicketStatus status, Long assigneeId, Long reporterId) {
        if (db == null) return Mono.just(0L);

        var spec = db.sql(SQL);

        // q (nullable String)
        if (q == null || q.isBlank()) {
            spec = spec.bindNull("q", String.class);
        } else {
            spec = spec.bind("q", q.trim());
        }

        // status (stored as VARCHAR)
        if (status == null) {
            spec = spec.bindNull("status", String.class);
        } else {
            spec = spec.bind("status", status.name());
        }

        // assigneeId (nullable BIGINT)
        if (assigneeId == null) {
            spec = spec.bindNull("assigneeId", Long.class);
        } else {
            spec = spec.bind("assigneeId", assigneeId);
        }

        // reporterId (nullable BIGINT)
        if (reporterId == null) {
            spec = spec.bindNull("reporterId", Long.class);
        } else {
            spec = spec.bind("reporterId", reporterId);
        }

        return spec.map((row, meta) -> {
                    Number n = (Number) row.get("cnt");
                    return (n == null) ? 0L : n.longValue();
                })
                .one();
    }
}
