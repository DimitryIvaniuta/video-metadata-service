package com.github.dimitryivaniuta.videometadata.web.dto.tickets;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCommentInput {
    private Long ticketId;
    private String body;
}