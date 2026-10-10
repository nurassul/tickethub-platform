package dev.project.booking.api.services;

import dev.project.booking.dto.BookingData;
import dev.project.booking.integration.kafka.service.BookingOutboxService;
import dev.project.booking.redis.service.RedisTaskService;
import dev.project.booking.repository.entity.BookingSeat;
import dev.project.booking.repository.entity.enums.BookingStatus;
import dev.project.booking.repository.entity.enums.RedisTaskOperation;
import dev.project.booking.repository.postgresql.BookingRepository;
import dev.project.booking.repository.postgresql.BookingSeatRepository;
import dev.project.booking.repository.postgresql.SeatReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingExpirationService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final BookingOutboxService bookingOutboxService;
    private final RedisTaskService redisTaskService;

    @Transactional
    public List<BookingData> expireBookings() {

        List<UUID> bookingIds =
                bookingRepository.findExpiredBookingIds(
                        BookingStatus.HOLD,
                        LocalDateTime.now(ZoneOffset.UTC),
                        PageRequest.of(0, 100)
                );

        List<BookingData> result = new ArrayList<>();


        for (UUID bookingId : bookingIds) {
            var optionalBooking = bookingRepository.findByIdForUpdate(bookingId);
            if (optionalBooking.isEmpty()) {
                continue;
            }
            var booking = optionalBooking.get();

            if (booking.getStatus() != BookingStatus.HOLD) {
                continue;
            }

            if (booking.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
                continue;
            }

            List<BookingSeat> seats = bookingSeatRepository
                    .findAllByBooking_Id(bookingId);



            booking.setStatus(BookingStatus.EXPIRED);

            seatReservationRepository.deleteAllByBooking_Id(
                    booking.getId()
            );

            bookingOutboxService.saveBookingExpired(booking);

            redisTaskService.enqueue(bookingId, RedisTaskOperation.RELEASE);

            result.add(new BookingData(
                    booking.getId(),
                    booking.getEventId(),
                    seats.stream()
                            .map(BookingSeat::getSeatId)
                            .toList(),
                    booking.getCustomerEmail()
            ));

        }


        return result;
    }
}
