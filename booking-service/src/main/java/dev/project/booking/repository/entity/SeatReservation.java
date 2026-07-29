package dev.project.booking.repository.entity;


import dev.project.booking.repository.entity.enums.SeatReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "seat_reservations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_active_event_seat",
                columnNames = {"event_id", "seat_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "seat_id", nullable = false)
    private UUID seatId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatReservationStatus status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
