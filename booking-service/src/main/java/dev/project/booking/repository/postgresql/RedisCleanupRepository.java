package dev.project.booking.repository.postgresql;

import dev.project.booking.repository.entity.RedisTask;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RedisCleanupRepository extends JpaRepository<RedisTask, UUID> {

    @Query(
            "SELECT r.id FROM RedisTask r WHERE r.nextAttemptAt <= :now ORDER BY r.nextAttemptAt, r.id"
    )
    List<UUID> findReadyTaskIds(@Param("now") Instant now, Pageable pageable);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT r FROM RedisTask r WHERE r.id=:taskId"
    )
    Optional<RedisTask> findByIdForUpdate(@Param("taskId") UUID taskId);


    @Modifying
    @Query(value = """
            INSERT INTO redis_tasks (
                id, booking_id, operation, created_at, next_attempt_at, attempts
            )
            VALUES (:id, :bookingId, :operation, :now, :now, 0)
            ON CONFLICT (booking_id, operation) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("bookingId") UUID bookingId,
                       @Param("operation") String operation,
                       @Param("now") Instant now);






}
