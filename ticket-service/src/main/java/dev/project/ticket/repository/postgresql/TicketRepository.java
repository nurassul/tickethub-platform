package dev.project.ticket.repository.postgresql;


import dev.project.ticket.repository.entity.Ticket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    boolean existsByBookingIdAndSeatId(UUID bookingId, UUID seatId);

    List<Ticket> findAllByBookingId(UUID bookingId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT t FROM Ticket t WHERE t.id=:ticketId"
    )
    Optional<Ticket> findByIdForUpdate(@Param("ticketId") UUID ticketId);
}
