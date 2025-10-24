package com.github.dimitryivaniuta.videometadata.repository;

import com.github.dimitryivaniuta.videometadata.model.Ticket;
import com.github.dimitryivaniuta.videometadata.model.TicketStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TicketRepository extends ReactiveCrudRepository<Ticket, Long>, TicketRepositoryCustom  {

    @Query("""
              SELECT * FROM tickets
              WHERE (:q IS NULL OR LOWER(title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(description) LIKE LOWER(CONCAT('%', :q, '%')))
                AND (:status IS NULL OR status = :status)
                AND (:assigneeId IS NULL OR assignee_id = :assigneeId)
                AND (:reporterId IS NULL OR reporter_id = :reporterId)
              ORDER BY created_at DESC
              LIMIT :limit OFFSET :offset
            """)
    Flux<Ticket> page(String q, TicketStatus status, Long assigneeId, Long reporterId, long limit, long offset);

}
