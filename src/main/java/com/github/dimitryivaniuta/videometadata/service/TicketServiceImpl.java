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

        int p = (page == null || page < 1) ? DEF_PG : page;
        int s = (pageSize == null || pageSize < 1) ? DEF_SZ : Math.min(pageSize, MAX_SZ);
        long off = (long) (p - 1) * s;
        String term = (search == null || search.isBlank()) ? null : search.trim();

        Mono<List<Ticket>> itemsMono =
                ticketRepo.page(term, status, assigneeId, reporterId, s, off)
                        .collectList();

        Mono<Long> totalMono =
                ticketRepo.countFiltered(term, status, assigneeId, reporterId);

        return itemsMono
                .zipWith(totalMono)
                .flatMap(tuple -> {
                    List<Ticket> tickets = tuple.getT1();
                    Long total = tuple.getT2();

                    // collect unique userIds from all tickets (reporter + assignee)
                    Set<Long> userIds = new HashSet<>();
                    for (Ticket t : tickets) {
                        if (t.getAssigneeId() != null) userIds.add(t.getAssigneeId());
                        if (t.getReporterId() != null) userIds.add(t.getReporterId());
                    }

                    return loadUsernameMap(userIds)
                            .map(nameMap -> {
                                List<TicketNode> nodes = tickets.stream()
                                        .map(t -> mapTicketNoCommentsWithNames(t, nameMap))
                                        .collect(Collectors.toList());

                                return TicketConnection.builder()
                                        .items(nodes)
                                        .page(p)
                                        .pageSize(s)
                                        .total(total)
                                        .build();
                            });
                });
    }

    /* ---------------------------------------------------------
     * GET BY ID
     * - can include comments
     * - enriches assigneeUsername, reporterUsername, and
     *   authorUsername for each comment
     * --------------------------------------------------------- */
    @Override
    public Mono<TicketNode> getById(Long id, boolean includeComments) {
        return ticketRepo.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Ticket not found")))
                .flatMap(ticketEntity -> {
                    if (!includeComments) {
                        // no comments -> still enrich with usernames
                        return enrichTicketWithUsernames(ticketEntity, List.of());
                    }

                    return commentRepo.findAllByTicketId(ticketEntity.getId())
                            .collectList()
                            .flatMap(comments -> enrichTicketWithUsernames(ticketEntity, comments));
                });
    }

    /**
     * Helper:
     * - Build a full TicketNode with usernames for reporter/assignee
     * - Build TicketCommentNode list with authorUsername
     * - We do one batched fetch of usernames.
     */
    private Mono<TicketNode> enrichTicketWithUsernames(
            Ticket t,
            List<TicketComment> commentEntities
    ) {
        // 1. collect all user IDs referenced
        Set<Long> userIds = new HashSet<>();
        if (t.getAssigneeId() != null) userIds.add(t.getAssigneeId());
        if (t.getReporterId() != null) userIds.add(t.getReporterId());
        for (TicketComment c : commentEntities) {
            if (c.getAuthorId() != null) {
                userIds.add(c.getAuthorId());
            }
        }

        // 2. fetch username map
        return loadUsernameMap(userIds)
                .map(nameMap -> {
                    // Build main node
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

                    // Build comment nodes with authorUsername
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
                                .collect(Collectors.toList());

                        nodeBuilder.comments(commentNodes);
                    }

                    return nodeBuilder.build();
                });
    }

    /**
     * Small helper to build TicketNode WITHOUT comments,
     * but WITH reporterUsername / assigneeUsername already looked up.
     * Used in the list(...) path.
     */
    private TicketNode mapTicketNoCommentsWithNames(Ticket t, Map<Long, String> nameMap) {
        return TicketNode.builder()
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
                .updatedAt(t.getUpdatedAt())
                // comments intentionally omitted in list
                .build();
    }

    /**
     * Utility to load username map for a set of userIds.
     * Returns Mono<Map<userId, username>>.
     * If the set is empty, we just return Mono.just(emptyMap()) to avoid hitting DB.
     */
    private Mono<Map<Long, String>> loadUsernameMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Mono.just(Map.of());
        }
        // userRepo.findAllByIdIn(userIds) must return Flux<User>
        return userRepo.findAllByIdIn(userIds)
                .collectMap(User::getId, User::getUsername);
    }

    /* ---------------------------------------------------------
     * CREATE
     * - reporterId is current user
     * - new ticket starts OPEN, unassigned
     * - we return enriched node (with usernames)
     * --------------------------------------------------------- */
    @Override
    public Mono<TicketNode> create(Long reporterId, TicketCreateInput in) {
        OffsetDateTime now = OffsetDateTime.now();

        Ticket t = Ticket.builder()
                .title(in.getTitle().trim())
                .description(in.getDescription())
                .priority(
                        in.getPriority() == null
                                ? TicketPriority.MEDIUM
                                : in.getPriority()
                )
                .status(TicketStatus.OPEN)
                .reporterId(reporterId)
                .assigneeId(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return ticketRepo.save(t)
                .flatMap(saved -> enrichTicketWithUsernames(saved, List.of()));
    }

    /* ---------------------------------------------------------
     * UPDATE
     * - current user (ownerId) is passed, you can add permissions if needed
     * - can change status, and assigneeId (including unassign = null)
     * - we return enriched node (with usernames)
     * --------------------------------------------------------- */
    @Override
    public Mono<TicketNode> update(Long ownerId, TicketUpdateInput in) {
        return ticketRepo.findById(in.getId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Ticket not found")))
                .flatMap(t -> {
                    if (in.getStatus() != null) {
                        t.setStatus(in.getStatus());
                    }
                    // we always set assigneeId (can assign or unassign)
                    t.setAssigneeId(in.getAssigneeId());

                    t.setUpdatedAt(OffsetDateTime.now());
                    return ticketRepo.save(t);
                })
                .flatMap(saved -> enrichTicketWithUsernames(saved, List.of()));
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
                .flatMap(savedComment -> {
                    // resolve author username just for this one id
                    return loadUsernameMap(Set.of(authorId))
                            .map(nameMap ->
                                    TicketCommentNode.builder()
                                            .id(savedComment.getId())
                                            .authorId(savedComment.getAuthorId())
                                            .authorUsername(
                                                    nameMap.getOrDefault(
                                                            savedComment.getAuthorId(),
                                                            null
                                                    )
                                            )
                                            .body(savedComment.getBody())
                                            .createdAt(savedComment.getCreatedAt())
                                            .build()
                            );
                });
    }

    /**
     * Legacy helper (kept for completeness / internal reuse)
     * NOT used for final responses anymore unless we explicitly want
     * a "bare" TicketNode without usernames or comments.
     */
    private TicketNode toNodeNoComments(Ticket t) {
        return TicketNode.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .status(t.getStatus())
                .priority(t.getPriority())
                .reporterId(t.getReporterId())
                .assigneeId(t.getAssigneeId())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
