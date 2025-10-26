package com.github.dimitryivaniuta.videometadata.service;

import com.github.dimitryivaniuta.videometadata.model.*;
import com.github.dimitryivaniuta.videometadata.repository.*;
import com.github.dimitryivaniuta.videometadata.web.dto.tickets.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final int DEF_PG = 1;
    private static final int DEF_SZ = 20;
    private static final int MAX_SZ = 100;

    private final TicketRepository ticketRepo;
    private final UserRepository userRepo;
    private final TicketCommentRepository commentRepo;

    /**
     * LIST (connectionTickets)
     * - returns page of TicketNode WITHOUT comments
     * - but WITH assigneeUsername / reporterUsername enriched
     */
    @Override
    public Mono<TicketConnection> list(Integer page,
                                       Integer pageSize,
                                       String search,
                                       TicketStatus status,
                                       Long assigneeId,
                                       Long reporterId) {

        int p = page == null || page < 1 ? DEF_PG : page;
        int s = pageSize == null || pageSize < 1 ? DEF_SZ : Math.min(pageSize, MAX_SZ);
        long off = (long) (p - 1) * s;
        String term = (search == null || search.isBlank()) ? null : search.trim();

        // load raw tickets and total
        Mono<List<Ticket>> itemsMono = ticketRepo
                .page(term, status, assigneeId, reporterId, s, off)
                .collectList();

        Mono<Long> totalMono = ticketRepo
                .countFiltered(term, status, assigneeId, reporterId);

        // enrich usernames in bulk
        return itemsMono
                .flatMap(this::enrichTicketListWithUsernames) // <─ changed
                .zipWith(totalMono)
                .map(tuple -> TicketConnection.builder()
                        .items(tuple.getT1())
                        .page(p)
                        .pageSize(s)
                        .total(tuple.getT2())
                        .build());
    }

    @Override
    public Mono<TicketNode> getById(Long id, boolean includeComments) {
        return ticketRepo.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Ticket not found")))
                .flatMap(ticketEntity -> {
                    if (!includeComments) {
                        // no comments -> enrich with usernames
                        return enrichSingleTicketWithUsernames(ticketEntity, List.of());
                    }
                    return commentRepo.findAllByTicketId(ticketEntity.getId())
                            .collectList()
                            .flatMap(comments ->
                                    enrichSingleTicketWithUsernames(ticketEntity, comments)
                            );
                });
    }

    /**
     * Helper:
     * - Build a full TicketNode with usernames for reporter/assignee
     * - Build TicketCommentNode list with authorUsername
     * - We do one batched fetch of usernames.
     */
    private Mono<List<TicketNode>> enrichTicketListWithUsernames(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            return Mono.just(List.of());
        }

        // collect all userIds across tickets (assignee+reporter)
        Set<Long> userIds = new HashSet<>();
        for (Ticket t : tickets) {
            if (t.getAssigneeId() != null) userIds.add(t.getAssigneeId());
            if (t.getReporterId() != null) userIds.add(t.getReporterId());
        }

        return loadUsernameMap(userIds)
                .map(map -> tickets.stream()
                        .map(t -> mapTicketAndComments(t, map, null))
                        .toList()
                );
    }

    private Mono<TicketNode> enrichSingleTicketWithUsernames(
            Ticket ticket,
            List<TicketComment> commentEntities
    ) {
        // gather IDs: ticket reporter/assignee + each comment author
        Set<Long> userIds = new HashSet<>();
        if (ticket.getAssigneeId() != null) userIds.add(ticket.getAssigneeId());
        if (ticket.getReporterId() != null) userIds.add(ticket.getReporterId());
        for (TicketComment c : commentEntities) {
            if (c.getAuthorId() != null) userIds.add(c.getAuthorId());
        }

        // 2. fetch username map
        return loadUsernameMap(userIds)
                .map(map -> mapTicketAndComments(ticket, map, commentEntities));
    }

    /**
     * Actually hits DB using the new safe custom repository method.
     */
    private Mono<Map<Long, String>> loadUsernameMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Mono.just(Map.of());
        }

        return userRepo.findAllByIds(userIds) // <─ custom impl
                .collectMap(User::getId, User::getUsername);
    }

    /**
     * Build a TicketNode + (optional) comments using cached username map.
     */
    private TicketNode mapTicketAndComments(
            Ticket t,
            Map<Long, String> usernames,
            List<TicketComment> commentEntitiesOrNull
    ) {
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
                                ? usernames.getOrDefault(t.getReporterId(), null)
                                : null
                )
                .assigneeUsername(
                        t.getAssigneeId() != null
                                ? usernames.getOrDefault(t.getAssigneeId(), null)
                                : null
                )
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt());

        if (commentEntitiesOrNull != null && !commentEntitiesOrNull.isEmpty()) {
            List<TicketCommentNode> commentNodes = commentEntitiesOrNull.stream()
                    .map(c -> TicketCommentNode.builder()
                            .id(c.getId())
                            .authorId(c.getAuthorId())
                            .authorUsername(
                                    c.getAuthorId() != null
                                            ? usernames.getOrDefault(c.getAuthorId(), null)
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
    }

    /* ----------------- rest of service unchanged ----------------- */

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

        return ticketRepo.save(t)
                .flatMap(saved -> enrichSingleTicketWithUsernames(saved, List.of()));
    }

    @Override
    public Mono<TicketNode> update(Long ownerId, TicketUpdateInput in) {
        return ticketRepo.findById(in.getId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Ticket not found")))
                .flatMap(t -> {
                    if (in.getStatus() != null) {
                        t.setStatus(in.getStatus());
                    }
                    // NOTE: we intentionally allow clearing assignee (null)
                    if (in.getAssigneeId() != null || in.getAssigneeId() == null) {
                        t.setAssigneeId(in.getAssigneeId());
                    }
                    t.setUpdatedAt(OffsetDateTime.now());
                    return ticketRepo.save(t);
                })
                .flatMap(saved -> enrichSingleTicketWithUsernames(saved, List.of()));
    }

    /* ---------------------------------------------------------
     * ADD COMMENT
     * - authorId = current user
     * - return the comment node enriched with authorUsername
     *   (so UI can immediately show "alice (123)" without refetch)
     * --------------------------------------------------------- */
    @Override
    public Mono<TicketCommentNode> addComment(Long authorId, TicketCommentInput in) {
        TicketComment c = TicketComment.builder()
                .ticketId(in.getTicketId())
                .authorId(authorId)
                .body(in.getBody())
                .createdAt(OffsetDateTime.now())
                .build();

        return commentRepo.save(c)
                .flatMap(saved -> {
                    // resolve author's username for response
                    return loadUsernameMap(Set.of(authorId))
                            .map(usernameMap -> TicketCommentNode.builder()
                                    .id(saved.getId())
                                    .authorId(saved.getAuthorId())
                                    .authorUsername(
                                            usernameMap.getOrDefault(authorId, null)
                                    )
                                    .body(saved.getBody())
                                    .createdAt(saved.getCreatedAt())
                                    .build());
                });
    }
}
