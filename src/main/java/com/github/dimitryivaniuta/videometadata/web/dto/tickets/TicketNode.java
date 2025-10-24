package com.github.dimitryivaniuta.videometadata.web.dto.tickets;

import com.github.dimitryivaniuta.videometadata.model.TicketPriority;
import com.github.dimitryivaniuta.videometadata.model.TicketStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class TicketNode {
    private Long id;
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private Long reporterId;
    private Long assigneeId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<TicketCommentNode> comments; // optional in detail
}