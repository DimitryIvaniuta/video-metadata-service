package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.model.*;
import com.github.dimitryivaniuta.videometadata.repository.*;
import com.github.dimitryivaniuta.videometadata.web.dto.tickets.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final int DEF_PG = 1, DEF_SZ = 20, MAX_SZ = 100;

    private final TicketRepository ticketRepo;
    private final UserRepository userRepo;
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
                .flatMap(ticketEntity -> {
                    if (!includeComments) {
                        // no comments => still enrich assigneeUsername / reporterUsername
                        return enrichTicketWithUsernames(ticketEntity, List.of());
                    }

                    // load comments first
                    return commentRepo.findAllByTicketId(ticketEntity.getId())
                            .collectList()
                            .flatMap(comments -> enrichTicketWithUsernames(ticketEntity, comments));
                });
    }

    private Mono<TicketNode> enrichTicketWithUsernames(
            Ticket t,
            List<TicketComment> commentEntities
    ) {
        // 1. collect all user IDs we need to resolve
        Set<Long> userIds = new HashSet<>();

        if (t.getAssigneeId() != null)    userIds.add(t.getAssigneeId());
        if (t.getReporterId() != null)    userIds.add(t.getReporterId());

        for (TicketComment c : commentEntities) {
            if (c.getAuthorId() != null) {
                userIds.add(c.getAuthorId());
            }
        }

        // 2. load (id -> username) map
        Mono<Map<Long,String>> usernamesMono = userRepo.findAllByIdIn(userIds)
                .collectMap(User::getId, User::getUsername);

        // 3. build the final TicketNode once we know usernames
        return usernamesMono.map(nameMap -> {
            // map main ticket first
            TicketNode.TicketNodeBuilder nodeBuilder = TicketNode.builder()
                    .id(t.getId())
                    .title(t.getTitle())
                    .description(t.getDescription())
                    .status(t.getStatus())
                    .priority(t.getPriority())
                    .reporterId(t.getReporterId())
                    .assigneeId(t.getAssigneeId())
                    .reporterUsername(
                            t.getReporterId() != null
                                    ? nameMap.getOrDefault(t.getReporterId(), null)
                                    : null
                    )
                    .assigneeUsername(
                            t.getAssigneeId() != null
                                    ? nameMap.getOrDefault(t.getAssigneeId(), null)
                                    : null
                    )
                    .createdAt(t.getCreatedAt())
                    .updatedAt(t.getUpdatedAt());

            // map comments (if provided)
            if (commentEntities != null && !commentEntities.isEmpty()) {
                List<TicketCommentNode> commentNodes = commentEntities.stream()
                        .map(c -> TicketCommentNode.builder()
                                .id(c.getId())
                                .authorId(c.getAuthorId())
                                .authorUsername(
                                        c.getAuthorId() != null
                                                ? nameMap.getOrDefault(c.getAuthorId(), null)
                                                : null
                                )
                                .body(c.getBody())
                                .createdAt(c.getCreatedAt())
                                .build()
                        )
                        .toList();
                nodeBuilder.comments(commentNodes);
            }

            return nodeBuilder.build();
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
    public Mono<TicketNode> update(Long ownerId, TicketUpdateInput in) {
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
