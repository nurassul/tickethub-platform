package dev.project.ticket.repository.entity;


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
@Table(name = "cancelled_bookings")
public class CancelledBooking {


    @Id
    private UUID bookingId;

    @Column(name = "cancelled_at", nullable = false)
    private Instant cancelledAt;
}
