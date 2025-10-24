package com.github.dimitryivaniuta.videometadata.web.dto.tickets;

import com.github.dimitryivaniuta.videometadata.model.TicketPriority;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCreateInput {
    private String title;
    private String description;
    private TicketPriority priority;
}