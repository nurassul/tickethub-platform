package dev.project.booking.repository.postgresql;

import dev.project.booking.repository.entity.BookingIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookingIdempotencyRepository extends JpaRepository<BookingIdempotency, UUID> {

    Optional<BookingIdempotency> findByGuestTokenHashAndIdempotencyKey(String guestTokenHash, String idempotencyKey);

}
