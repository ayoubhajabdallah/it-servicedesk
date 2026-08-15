package dev.ayoub.servicedesk.service;

import dev.ayoub.servicedesk.domain.Ticket;
import dev.ayoub.servicedesk.domain.TicketStatus;
import dev.ayoub.servicedesk.domain.User;
import dev.ayoub.servicedesk.domain.UserRole;
import dev.ayoub.servicedesk.dto.TicketResponse;
import dev.ayoub.servicedesk.repository.TicketRepository;
import dev.ayoub.servicedesk.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public TicketResponse createTicket(Ticket ticket) {

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

        Ticket saved = ticketRepository.save(ticket);

        return TicketResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets() {

        return ticketRepository.findAll()
                .stream()
                .map(TicketResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id) {

        Ticket ticket = findTicket(id);

        return TicketResponse.from(ticket);
    }

    @Transactional
    public TicketResponse updateStatus(Long id, TicketStatus status) {

        Ticket ticket = findTicket(id);
        ticket.setStatus(status);

        Ticket saved = ticketRepository.save(ticket);

        return TicketResponse.from(saved);
    }

    @Transactional
    public TicketResponse assignTechnician(Long ticketId, Long userId) {

        Ticket ticket = findTicket(ticketId);

        User technician = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found: " + userId));

        if (technician.getRole() != UserRole.TECHNICIAN) {
            throw new IllegalArgumentException(
                    "Selected user is not a technician"
            );
        }

        ticket.setAssignedTo(technician);

        Ticket saved = ticketRepository.save(ticket);

        return TicketResponse.from(saved);
    }

    @Transactional
    public void deleteTicket(Long id) {

        Ticket ticket = findTicket(id);
        ticketRepository.delete(ticket);
    }

    private Ticket findTicket(Long id) {

        return ticketRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Ticket not found: " + id));
    }
}