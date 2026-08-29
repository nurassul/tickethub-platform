package dev.project.booking.api.services;

import dev.project.booking.dto.BookingData;
import dev.project.booking.repository.entity.Booking;
import dev.project.booking.repository.entity.BookingSeat;
import dev.project.booking.repository.entity.enums.BookingStatus;
import dev.project.booking.repository.postgresql.BookingRepository;
import dev.project.booking.repository.postgresql.BookingSeatRepository;
import dev.project.booking.repository.postgresql.SeatReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingExpirationService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatReservationRepository seatReservationRepository;

    @Transactional
    public List<BookingData> expireBookings() {
        Page<Booking> page =
                bookingRepository.findAllByStatusAndExpiresAtLessThanEqual(
                        BookingStatus.HOLD,
                        LocalDateTime.now(),
                        PageRequest.of(0, 100)
                );

        List<BookingData> result = new ArrayList<>();

        for (Booking booking : page.getContent()) {
            List<BookingSeat> seats =
                    bookingSeatRepository.findAllByBooking_Id(
                            booking.getId()
                    );

            booking.setStatus(BookingStatus.EXPIRED);

            seatReservationRepository.deleteAllByBooking_Id(
                    booking.getId()
            );

            result.add(new BookingData(
                    booking.getId(),
                    booking.getEventId(),
                    seats.stream()
                            .map(BookingSeat::getSeatId)
                            .toList()
            ));
        }

        return result;
    }
}
