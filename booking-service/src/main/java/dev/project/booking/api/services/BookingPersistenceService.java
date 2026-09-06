package dev.project.booking.api.services;


import dev.project.booking.dto.*;
import dev.project.booking.dto.feign.ValidatedSeatsResponse;
import dev.project.booking.repository.entity.Booking;
import dev.project.booking.repository.entity.BookingSeat;
import dev.project.booking.repository.entity.SeatReservation;
import dev.project.booking.repository.entity.enums.BookingStatus;
import dev.project.booking.repository.entity.enums.SeatReservationStatus;
import dev.project.booking.repository.postgresql.BookingRepository;
import dev.project.booking.repository.postgresql.BookingSeatRepository;
import dev.project.booking.repository.postgresql.SeatReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingPersistenceService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatReservationRepository seatReservationRepository;


    @Transactional
    public BookingResponse create(
            UUID bookingId,
            CreateBookingRequest request,
            ValidatedSeatsResponse validated
    ) {
        LocalDateTime expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10);

        Booking booking = Booking.builder()
                .id(bookingId)
                .eventId(request.eventId())
                .customerEmail(request.customerEmail())
                .customerPhone(request.customerPhone())
                .expiresAt(expiresAt)
                .status(BookingStatus.HOLD)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        List<BookingSeat> bookingSeats = validated.seats().stream()
                .map(seat -> BookingSeat.builder()
                        .booking(savedBooking)
                        .seatId(seat.seatId())
                        .priceAtBooking(seat.price())
                        .build()
                )
                .toList();

        bookingSeatRepository.saveAll(bookingSeats);


        List<SeatReservation> reservations = validated.seats().stream()
                .map(seat -> SeatReservation.builder()
                        .booking(savedBooking)
                        .eventId(request.eventId())
                        .seatId(seat.seatId())
                        .status(SeatReservationStatus.HOLD)
                        .expiresAt(expiresAt)
                        .build()
                )
                .toList();

        seatReservationRepository.saveAll(reservations);
        seatReservationRepository.flush();

        return toResponse(savedBooking, bookingSeats);
    }


    @Transactional(readOnly = true)
    public BookingResponse findById(UUID bookingId) {
        Booking booking = getBooking(bookingId);

        List<BookingSeat> seats = bookingSeatRepository.findAllByBooking_Id(bookingId);

        return toResponse(booking, seats);
    }


    @Transactional
    public BookingData cancel(UUID bookingId) {
        Booking booking = getBooking(bookingId);

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Confirmed booking cannot be cancelled"
            );
        }

        if (booking.getStatus() == BookingStatus.EXPIRED) {
            throw new IllegalStateException(
                    "Booking has already expired"
            );
        }

        List<BookingSeat> seats =
                bookingSeatRepository.findAllByBooking_Id(bookingId);

        booking.setStatus(BookingStatus.CANCELLED);

        seatReservationRepository.deleteAllByBooking_Id(bookingId);

        return new BookingData(
                booking.getId(),
                booking.getEventId(),
                seats.stream().map(BookingSeat::getSeatId).toList()
        );
    }


    @Transactional
    public BookingData confirm(UUID bookingId) {
        Booking booking = getBooking(bookingId);

        if (booking.getStatus() != BookingStatus.HOLD) {
            throw new IllegalStateException(
                    "Booking status is not 'HOLD'"
            );
        }

        if (!booking.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Booking has expired");
        }

        List<BookingSeat> seats =
                bookingSeatRepository.findAllByBooking_Id(bookingId);

        List<SeatReservation> reservations =
                seatReservationRepository.findAllByBooking_Id(bookingId);

        if (reservations.size() != seats.size()) {
            throw new IllegalStateException(
                    "Booking reservations are inconsistent"
            );
        }

        booking.setStatus(BookingStatus.CONFIRMED);

        reservations.forEach(reservation -> {
            reservation.setStatus(SeatReservationStatus.CONFIRMED);
            reservation.setExpiresAt(null);
        });

        return new BookingData(
                booking.getId(),
                booking.getEventId(),
                seats.stream().map(BookingSeat::getSeatId).toList()
        );
    }

    @Transactional
    public BookingData failPayment(UUID bookingId) {
        Booking booking = getBooking(bookingId);

        if (booking.getStatus() != BookingStatus.HOLD) {
            throw new IllegalStateException(
                    "Booking status is not 'HOLD'"
            );
        }

        List<BookingSeat> seats =
                bookingSeatRepository.findAllByBooking_Id(bookingId);

        List<SeatReservation> reservations =
                seatReservationRepository.findAllByBooking_Id(bookingId);

        if (reservations.size() != seats.size()) {
            throw new IllegalStateException(
                    "Booking reservations are inconsistent"
            );
        }

        booking.setStatus(BookingStatus.PAYMENT_FAILED);

        seatReservationRepository.deleteAllByBooking_Id(bookingId);

        return new BookingData(
                booking.getId(),
                booking.getEventId(),
                seats.stream().map(BookingSeat::getSeatId).toList()
        );
    }


    @Transactional(readOnly = true)
    public BookingPaymentData getPaymentData(
            UUID bookingId
    ) {
        var booking = getBooking(bookingId);

        if (booking.getStatus() != BookingStatus.HOLD) {
            throw new IllegalStateException("Booking status is not 'HOLD'");
        }

        if (!booking.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Booking is expired");
        }

        List<BookingSeat> seats =
                bookingSeatRepository.findAllByBooking_Id(bookingId);

        if (seats.isEmpty()) {
            throw new IllegalStateException("Booking has no seats!");
        }

        BigDecimal totalPrice = seats.stream()
                .map(BookingSeat::getPriceAtBooking)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BookingPaymentData(
                booking.getId(),
                booking.getStatus(),
                booking.getExpiresAt(),
                totalPrice
        );

    }




    private Booking getBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Booking not found with id=" + bookingId
                ));
    }

    private BookingResponse toResponse(
            Booking booking,
            List<BookingSeat> seats
    ) {
        List<BookingSeatResponse> seatResponses = seats.stream()
                .map(seat -> new BookingSeatResponse(
                        seat.getSeatId(),
                        seat.getPriceAtBooking()
                ))
                .toList();

        BigDecimal totalPrice = seats.stream()
                .map(BookingSeat::getPriceAtBooking)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BookingResponse(
                booking.getId(),
                booking.getCustomerEmail(),
                booking.getCustomerPhone(),
                booking.getEventId(),
                seatResponses,
                booking.getStatus(),
                booking.getExpiresAt(),
                totalPrice
        );
    }


}
