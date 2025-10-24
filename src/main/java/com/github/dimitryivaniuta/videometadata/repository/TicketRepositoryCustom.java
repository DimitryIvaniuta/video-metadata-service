package com.github.dimitryivaniuta.videometadata.repository;

import com.github.dimitryivaniuta.videometadata.model.TicketStatus;
import reactor.core.publisher.Mono;

public interface TicketRepositoryCustom {
    Mono<Long> countFiltered(String q, TicketStatus status, Long assigneeId, Long reporterId);
}