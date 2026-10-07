package dev.project.ticket.repository.postgresql;


import dev.project.ticket.repository.entity.CancelledBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CancelledBookingsRepository extends JpaRepository<CancelledBooking, UUID> {

}
