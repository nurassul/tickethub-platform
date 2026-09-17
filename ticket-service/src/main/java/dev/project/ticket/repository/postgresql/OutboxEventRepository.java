package dev.project.ticket.repository.postgresql;

import dev.project.ticket.repository.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent>
    findTop100ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            Instant now
    );


    @Transactional
    @Modifying
    @Query("""
                UPDATE OutboxEvent event
                        SET event.publishedAt = :publishedAt,
                                event.lastError = null
                        WHERE event.id = :eventId
                          AND event.publishedAt IS NULL
            """)
    int markPublished(
            @Param("eventId") UUID eventId,
            @Param("publishedAt") Instant publishedAt);

    @Transactional
    @Modifying
    @Query("""
            UPDATE OutboxEvent event
            SET event.attempts = event.attempts + 1,
                event.lastError = :error,
                event.nextAttemptAt = :nextAttemptAt
            WHERE event.id = :eventId
              AND event.publishedAt IS NULL
            """)
    int markFailed(
            @Param("eventId") UUID eventId,
            @Param("error") String error,
            @Param("nextAttemptAt") Instant nextAttemptAt
    );

}
