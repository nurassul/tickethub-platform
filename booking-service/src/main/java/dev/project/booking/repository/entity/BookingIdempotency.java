package dev.project.booking.repository.entity;


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
@Table(name = "booking_idempotency")
public class BookingIdempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "guest_token_hash", length = 64, nullable = false)
    private String guestTokenHash;


    @Column(name = "idempotency_key", length = 255, nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64, nullable = false)
    private String requestHash;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;


    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT", nullable = false)
    private String responseBody;


    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;


    @PrePersist
    void onCreate() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }
    }


}
