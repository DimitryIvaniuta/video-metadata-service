package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.model.Ticket;
import com.github.dimitryivaniuta.videometadata.model.TicketComment;
import com.github.dimitryivaniuta.videometadata.model.TicketPriority;
import com.github.dimitryivaniuta.videometadata.model.TicketStatus;
import com.github.dimitryivaniuta.videometadata.repository.*;
import com.github.dimitryivaniuta.videometadata.web.dto.tickets.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final int DEF_PG = 1, DEF_SZ = 20, MAX_SZ = 100;

    private final TicketRepository ticketRepo;
    private final TicketCommentRepository commentRepo;

    @Override
    public Mono<TicketConnection> list(Integer page, Integer pageSize, String search,
                                       TicketStatus status, Long assigneeId, Long reporterId) {
        int p = page == null || page < 1 ? DEF_PG : page;
        int s = pageSize == null || pageSize < 1 ? DEF_SZ : Math.min(pageSize, MAX_SZ);
        long off = (long) (p - 1) * s;
        String term = (search == null || search.isBlank()) ? null : search.trim();

        var itemsMono = ticketRepo.page(term, status, assigneeId, reporterId, s, off).collectList();
        var totalMono = ticketRepo.countFiltered(term, status, assigneeId, reporterId);

        return itemsMono.zipWith(totalMono)
                .map(t -> TicketConnection.builder()
                        .items(t.getT1().stream().map(this::toNodeNoComments).toList())
                        .page(p)
                        .pageSize(s)
                        .total(t.getT2())
                        .build());
    }

    @Override
    public Mono<TicketNode> getById(Long id, boolean includeComments) {
        return ticketRepo.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Ticket not found")))
                .flatMap(t -> {
                    if (!includeComments) return Mono.just(toNodeNoComments(t));
                    return commentRepo.findAllByTicketId(t.getId())
                            .map(c -> TicketCommentNode.builder()
                                    .id(c.getId()).authorId(c.getAuthorId()).body(c.getBody()).createdAt(c.getCreatedAt()).build())
                            .collectList()
                            .map(list -> toNodeNoComments(t).toBuilder().comments(list).build());
                });
    }

    @Override
    public Mono<TicketNode> create(Long reporterId, TicketCreateInput in) {
        OffsetDateTime now = OffsetDateTime.now();
        Ticket t = Ticket.builder()
                .title(in.getTitle().trim())
                .description(in.getDescription())
                .priority(in.getPriority() == null ? TicketPriority.MEDIUM : in.getPriority())
                .status(TicketStatus.OPEN)
                .reporterId(reporterId)
                .assigneeId(null)
                .createdAt(now).updatedAt(now)
                .build();
        return ticketRepo.save(t).map(this::toNodeNoComments);
    }

    @Override
    public Mono<TicketNode> update(TicketUpdateInput in) {
        return ticketRepo.findById(in.getId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Ticket not found")))
                .flatMap(t -> {
                    if (in.getStatus() != null) t.setStatus(in.getStatus());
                    if (in.getAssigneeId() != null || in.getAssigneeId() == null) t.setAssigneeId(in.getAssigneeId());
                    t.setUpdatedAt(OffsetDateTime.now());
                    return ticketRepo.save(t);
                }).map(this::toNodeNoComments);
    }

    @Override
    public Mono<TicketCommentNode> addComment(Long authorId, TicketCommentInput in) {
        TicketComment c = TicketComment.builder()
                .ticketId(in.getTicketId())
                .authorId(authorId)
                .body(in.getBody())
                .createdAt(OffsetDateTime.now())
                .build();
        return commentRepo.save(c).map(x -> TicketCommentNode.builder()
                .id(x.getId()).authorId(x.getAuthorId()).body(x.getBody()).createdAt(x.getCreatedAt()).build());
    }

    private TicketNode toNodeNoComments(Ticket t) {
        return TicketNode.builder()
                .id(t.getId()).title(t.getTitle()).description(t.getDescription())
                .status(t.getStatus()).priority(t.getPriority())
                .reporterId(t.getReporterId()).assigneeId(t.getAssigneeId())
                .createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt())
                .build();
    }
}
