package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.model.TicketStatus;
import com.github.dimitryivaniuta.videometadata.web.dto.tickets.*;
import reactor.core.publisher.Mono;

public interface TicketService {
    Mono<TicketConnection> list(Integer page, Integer pageSize, String search, TicketStatus status, Long assigneeId, Long reporterId);

    Mono<TicketNode> getById(Long id, boolean includeComments);

    Mono<TicketNode> create(Long reporterId, TicketCreateInput in);

    Mono<TicketNode> update(Long ownerId, TicketUpdateInput in);

    Mono<TicketCommentNode> addComment(Long authorId, TicketCommentInput in);
}