package dev.ayoub.servicedesk.dto;

import dev.ayoub.servicedesk.domain.TicketCategory;
import dev.ayoub.servicedesk.domain.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 3000) String description,
        @NotNull TicketCategory category,
        @NotNull TicketPriority priority
) {
}
