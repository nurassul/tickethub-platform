package dev.project.event.repository;

import dev.project.event.repository.entity.Event;
import dev.project.event.repository.entity.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findAllByStatus(EventStatus status, Pageable pageable);

}
