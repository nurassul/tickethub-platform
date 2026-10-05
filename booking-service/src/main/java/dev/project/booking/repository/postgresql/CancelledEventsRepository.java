package dev.project.booking.repository.postgresql;

import dev.project.booking.repository.entity.CancelledEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CancelledEventsRepository extends JpaRepository<CancelledEvent, UUID> {
}
