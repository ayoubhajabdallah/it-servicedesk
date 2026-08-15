package dev.ayoub.servicedesk.service;

import dev.ayoub.servicedesk.domain.Ticket;
import dev.ayoub.servicedesk.domain.TicketStatus;
import dev.ayoub.servicedesk.domain.User;
import dev.ayoub.servicedesk.repository.TicketRepository;
import dev.ayoub.servicedesk.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketService(
            TicketRepository ticketRepository,
            UserRepository userRepository) {

        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    public Ticket createTicket(Ticket ticket) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User creator = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Authenticated user not found"));

        ticket.setId(null);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(creator);
        ticket.setAssignedTo(null);

        return ticketRepository.save(ticket);
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Ticket not found: " + id));
    }

    public Ticket updateStatus(Long id, TicketStatus status) {

        Ticket ticket = getTicketById(id);
        ticket.setStatus(status);

        return ticketRepository.save(ticket);
    }

    public void deleteTicket(Long id) {

        Ticket ticket = getTicketById(id);
        ticketRepository.delete(ticket);
    }
}