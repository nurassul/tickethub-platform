package dev.project.booking.repository.postgresql;

import dev.project.booking.repository.entity.SeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeatReservationRepository
        extends JpaRepository<SeatReservation, UUID> {

    List<SeatReservation> findAllByBooking_Id(UUID bookingId);

    void deleteAllByBooking_Id(UUID bookingId);

    Optional<SeatReservation> findByBooking_IdAndSeatId(
            UUID bookingId,
            UUID seatId
    );
}
