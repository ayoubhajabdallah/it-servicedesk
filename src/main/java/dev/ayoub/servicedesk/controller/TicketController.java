package dev.ayoub.servicedesk.controller;

import dev.ayoub.servicedesk.dto.CreateTicketRequest;
import dev.ayoub.servicedesk.domain.TicketStatus;
import dev.ayoub.servicedesk.dto.TicketResponse;
import dev.ayoub.servicedesk.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse createTicket(@Valid @RequestBody CreateTicketRequest request) {
        return ticketService.createTicket(request);
    }

    @GetMapping
    public List<TicketResponse> getAllTickets() {
        return ticketService.getAllTickets();
    }

    @GetMapping("/{id}")
    public TicketResponse getTicketById(@PathVariable Long id) {
        return ticketService.getTicketById(id);
    }

    @PatchMapping("/{id}/status")
    public TicketResponse updateStatus(
            @PathVariable Long id,
            @RequestParam TicketStatus status) {

        return ticketService.updateStatus(id, status);
    }

    @PatchMapping("/{id}/assign/{userId}")
    public TicketResponse assignTechnician(
            @PathVariable Long id,
            @PathVariable Long userId) {

        return ticketService.assignTechnician(id, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
    }
}
