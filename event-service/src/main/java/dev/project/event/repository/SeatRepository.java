package dev.project.event.repository;

import dev.project.event.repository.entity.Event;
import dev.project.event.repository.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {
    boolean existsByEventId(UUID eventId);

    List<Seat> findAllByEvent(Event event);
}
