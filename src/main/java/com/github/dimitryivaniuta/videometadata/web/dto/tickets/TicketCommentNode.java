package com.github.dimitryivaniuta.videometadata.web.dto.tickets;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketCommentNode {
    private Long id;
    private Long authorId;
    private String body;
    private OffsetDateTime createdAt;
}