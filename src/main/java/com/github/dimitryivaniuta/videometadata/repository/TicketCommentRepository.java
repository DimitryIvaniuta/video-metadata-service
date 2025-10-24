package com.github.dimitryivaniuta.videometadata.repository;

import com.github.dimitryivaniuta.videometadata.model.TicketComment;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface TicketCommentRepository extends ReactiveCrudRepository<TicketComment, Long> {
    @Query("SELECT * FROM ticket_comments WHERE ticket_id = :ticketId ORDER BY created_at ASC")
    Flux<TicketComment> findAllByTicketId(Long ticketId);
}