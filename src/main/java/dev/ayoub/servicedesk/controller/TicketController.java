package dev.ayoub.servicedesk.controller;

import dev.ayoub.servicedesk.domain.Ticket;
import dev.ayoub.servicedesk.domain.TicketStatus;
import dev.ayoub.servicedesk.dto.TicketResponse;
import dev.ayoub.servicedesk.service.TicketService;
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
    public TicketResponse createTicket(@RequestBody Ticket ticket) {

        Ticket created = ticketService.createTicket(ticket);

        return TicketResponse.from(created);
    }

    @GetMapping
    public List<TicketResponse> getAllTickets() {

        return ticketService.getAllTickets()
                .stream()
                .map(TicketResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TicketResponse getTicketById(@PathVariable Long id) {

        return TicketResponse.from(
                ticketService.getTicketById(id)
        );
    }

    @PatchMapping("/{id}/status")
    public TicketResponse updateStatus(
            @PathVariable Long id,
            @RequestParam TicketStatus status) {

        return TicketResponse.from(
                ticketService.updateStatus(id, status)
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
    }
}