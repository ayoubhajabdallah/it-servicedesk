package dev.ayoub.servicedesk.dto;

import dev.ayoub.servicedesk.domain.*;

import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketCategory category,
        TicketPriority priority,
        TicketStatus status,
        Long createdById,
        String createdByName,
        Long assignedToId,
        String assignedToName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static TicketResponse from(Ticket ticket) {

        User creator = ticket.getCreatedBy();
        User assignee = ticket.getAssignedTo();

        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getStatus(),

                creator != null ? creator.getId() : null,
                creator != null ? creator.getName() : null,

                assignee != null ? assignee.getId() : null,
                assignee != null ? assignee.getName() : null,

                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}