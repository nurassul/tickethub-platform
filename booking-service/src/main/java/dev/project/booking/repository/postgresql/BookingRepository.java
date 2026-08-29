package dev.project.booking.repository.postgresql;


import dev.project.booking.repository.entity.Booking;
import dev.project.booking.repository.entity.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID>{

    Page<Booking> findAllByStatusAndExpiresAtLessThanEqual(
            BookingStatus status,
            LocalDateTime expiresAt,
            Pageable pageable
    );

}
