package dev.project.booking.repository.postgresql;

import dev.project.booking.repository.entity.RedisCleanupTask;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RedisCleanupRepository extends JpaRepository<RedisCleanupTask, UUID> {

    @Query(
            "SELECT r.bookingId FROM RedisCleanupTask r WHERE r.nextAttemptAt <= :now ORDER BY r.nextAttemptAt, r.bookingId"
    )
    List<UUID> findReadyBookingIds(@Param("now") Instant now, Pageable pageable);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT r FROM RedisCleanupTask r WHERE r.bookingId=:bookingId"
    )
    Optional<RedisCleanupTask> findByBookingIdForUpdate(@Param("bookingId") UUID bookingId);
}
