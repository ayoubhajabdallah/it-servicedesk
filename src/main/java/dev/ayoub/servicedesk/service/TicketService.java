package dev.ayoub.servicedesk.service;

import dev.ayoub.servicedesk.domain.Ticket;
import dev.ayoub.servicedesk.domain.TicketStatus;
import dev.ayoub.servicedesk.domain.User;
import dev.ayoub.servicedesk.domain.UserRole;
import dev.ayoub.servicedesk.dto.TicketResponse;
import dev.ayoub.servicedesk.dto.CreateTicketRequest;
import dev.ayoub.servicedesk.repository.TicketRepository;
import dev.ayoub.servicedesk.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
    public TicketResponse createTicket(CreateTicketRequest request) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User creator = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

        Ticket ticket = Ticket.builder()
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .priority(request.priority())
                .status(TicketStatus.OPEN)
                .createdBy(creator)
                .assignedTo(null)
                .build();

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
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));

        if (technician.getRole() != UserRole.TECHNICIAN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Selected user is not a technician"
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
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found: " + id));
    }
}
