package com.github.dimitryivaniuta.videometadata.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table("ticket_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketComment {
    @Id
    private Long id;
    private Long ticketId;
    private Long authorId;
    private String body;
    private OffsetDateTime createdAt;
}