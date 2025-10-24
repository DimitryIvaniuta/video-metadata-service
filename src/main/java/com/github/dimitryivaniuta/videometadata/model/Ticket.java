package com.github.dimitryivaniuta.videometadata.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table("tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    @Id
    private Long id;
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private Long reporterId;
    private Long assigneeId;           // nullable
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}