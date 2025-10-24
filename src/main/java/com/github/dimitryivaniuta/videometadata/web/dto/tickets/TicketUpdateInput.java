package com.github.dimitryivaniuta.videometadata.web.dto.tickets;

import com.github.dimitryivaniuta.videometadata.model.TicketStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketUpdateInput {
    private Long id;
    private TicketStatus status;
    private Long assigneeId; // nullable (unassign if null present)
}