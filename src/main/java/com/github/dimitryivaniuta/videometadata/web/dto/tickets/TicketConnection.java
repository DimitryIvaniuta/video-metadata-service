package com.github.dimitryivaniuta.videometadata.web.dto.tickets;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketConnection {
    private List<TicketNode> items;
    private int page;
    private int pageSize;
    private long total;
}