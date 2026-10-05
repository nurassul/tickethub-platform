package dev.project.booking.repository.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cancelled_events")
public class CancelledEvent {

    @Id
    private UUID eventId;

    @Column(name = "cancelled_at", nullable = false)
    private Instant cancelledAt;


}


