package dev.ayoub.servicedesk.repository;

import dev.ayoub.servicedesk.domain.Ticket;
import dev.ayoub.servicedesk.domain.TicketStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Override
    @EntityGraph(attributePaths = {"createdBy", "assignedTo"})
    List<Ticket> findAll();

    @Override
    @EntityGraph(attributePaths = {"createdBy", "assignedTo"})
    Optional<Ticket> findById(Long id);

    List<Ticket> findByStatus(TicketStatus status);
}