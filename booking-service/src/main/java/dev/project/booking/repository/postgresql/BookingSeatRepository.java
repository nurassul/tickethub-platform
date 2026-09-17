package dev.project.booking.repository.postgresql;


import dev.project.booking.repository.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, UUID> {

    List<BookingSeat> findAllByBooking_Id(UUID bookingId);

    Optional<BookingSeat> findByBooking_IdAndSeatId(
            UUID bookingId,
            UUID seatId
    );

}
