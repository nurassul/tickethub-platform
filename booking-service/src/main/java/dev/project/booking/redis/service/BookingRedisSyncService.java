package dev.project.booking.redis.service;


import dev.project.booking.repository.entity.BookingSeat;
import dev.project.booking.repository.entity.enums.BookingSeatStatus;
import dev.project.booking.repository.entity.enums.BookingStatus;
import dev.project.booking.repository.postgresql.BookingRepository;
import dev.project.booking.repository.postgresql.BookingSeatRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingRedisSyncService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatHoldService seatHoldService;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSoldIfActive(UUID bookingId) {
        var booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: id = " + bookingId));

        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PARTIALLY_CANCELLED) {
            return;
        }

        var seats = bookingSeatRepository.findAllByBooking_Id(bookingId);

        var seatIds = seats.stream()
                .filter(seat -> seat.getStatus() == BookingSeatStatus.ACTIVE)
                .map(BookingSeat::getSeatId)
                .toList();

        if (seatIds.isEmpty()) {
            return;
        }

        seatHoldService.markSold(bookingId, booking.getEventId(), seatIds);
    }

}
