package com.github.dimitryivaniuta.videometadata.web.graphql;

import com.github.dimitryivaniuta.videometadata.graphql.annotations.GraphQLApplication;
import com.github.dimitryivaniuta.videometadata.graphql.annotations.GraphQLArgument;
import com.github.dimitryivaniuta.videometadata.graphql.annotations.GraphQLField;
import com.github.dimitryivaniuta.videometadata.graphql.annotations.GraphQLMutation;
import com.github.dimitryivaniuta.videometadata.model.TicketStatus;
import com.github.dimitryivaniuta.videometadata.service.TicketService;
import com.github.dimitryivaniuta.videometadata.web.dto.tickets.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@GraphQLApplication
public class TicketOperations {

    private final TicketService ticketService;

    @GraphQLField("connectionTickets")
    // @RequiresRole({"USER","ADMIN"})
    public Mono<TicketConnection> tickets(
            @GraphQLArgument("page") Integer page,
            @GraphQLArgument("pageSize") Integer pageSize,
            @GraphQLArgument("search") String search,
            @GraphQLArgument("status") TicketStatus status,
            @GraphQLArgument("assigneeId") Long assigneeId,
            @GraphQLArgument("reporterId") Long reporterId
    ) {
        return ticketService.list(page, pageSize, search, status, assigneeId, reporterId);
    }

    @GraphQLField("ticket")
    // @RequiresRole({"USER","ADMIN"})
    public Mono<TicketNode> ticket(@GraphQLArgument("id") Long id,
                                   @GraphQLArgument("includeComments") Boolean includeComments) {
        return ticketService.getById(id, Boolean.TRUE.equals(includeComments));
    }

    /* ───── MUTATIONS ───── */

    @GraphQLMutation("createTicket")
    // @RequiresRole({"USER","ADMIN"})
    public Mono<TicketNode> createTicket(@GraphQLArgument("reporterId") Long reporterId,
                                         @GraphQLArgument("input") TicketCreateInput input) {
        return ticketService.create(reporterId, input);
    }

    @GraphQLMutation("updateTicket")
    // @RequiresRole({"USER","ADMIN"})
    public Mono<TicketNode> updateTicket(@GraphQLArgument("input") TicketUpdateInput input) {
        return ticketService.update(input);
    }

    @GraphQLMutation("addTicketComment")
    // @RequiresRole({"USER","ADMIN"})
    public Mono<TicketCommentNode> addTicketComment(@GraphQLArgument("authorId") Long authorId,
                                                    @GraphQLArgument("input") TicketCommentInput input) {
        return ticketService.addComment(authorId, input);
    }
}
