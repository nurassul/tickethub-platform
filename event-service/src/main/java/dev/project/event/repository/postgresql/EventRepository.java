package dev.project.event.repository.postgresql;

import dev.project.event.repository.entity.Event;
import dev.project.event.repository.entity.enums.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findAllByStatus(EventStatus status, Pageable pageable);
    List<Event> findAllByIdInAndStatus(List<UUID> eventIds, EventStatus eventStatus);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT e FROM Event e WHERE e.id=:eventId"
    )
    Optional<Event> findByIdForUpdate(@Param("eventId") UUID eventId);
}
