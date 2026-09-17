package dev.project.ticket.repository.postgresql;


import dev.project.ticket.repository.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    boolean existsByBookingIdAndSeatId(UUID bookingId, UUID seatId);

    List<Ticket> findAllByBookingId(UUID bookingId);
    
}
