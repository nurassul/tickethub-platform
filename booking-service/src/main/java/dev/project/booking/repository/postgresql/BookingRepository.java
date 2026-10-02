package dev.project.booking.repository.postgresql;


import dev.project.booking.repository.entity.Booking;
import dev.project.booking.repository.entity.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID>{

    Page<Booking> findAllByStatusAndExpiresAtLessThanEqual(
            BookingStatus status,
            LocalDateTime expiresAt,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id=:bookingId")
    Optional<Booking> findByIdForUpdate(@Param("bookingId") UUID bookingId);


    @Query("SELECT b.id FROM Booking b WHERE b.status=:status AND b.expiresAt <= :cutoff ORDER BY b.id")
    List<UUID> findExpiredBookingIds(
            @Param("status") BookingStatus status,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable);

}
