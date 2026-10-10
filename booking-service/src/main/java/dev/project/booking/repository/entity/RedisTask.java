package dev.project.booking.repository.entity;


import dev.project.booking.repository.entity.enums.RedisTaskOperation;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "redis_tasks")
public class RedisTask {

    @Id
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false)
    private RedisTaskOperation operation;








}
