package dev.project.booking.repository.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "booking_seats",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_booking_seat",
                columnNames = {"booking_id", "seat_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSeat {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "booking_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_booking_seats_booking")
    )
    private Booking booking;

    @Column(name = "seat_id", nullable = false)
    private UUID seatId;

    @Column(
            name = "price_at_booking",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal priceAtBooking;
}
