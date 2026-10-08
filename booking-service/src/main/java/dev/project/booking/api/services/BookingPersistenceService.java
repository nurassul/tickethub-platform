package dev.project.booking.api.services;


import dev.project.booking.api.exceptions.BusinessConflictException;
import dev.project.booking.dto.*;
import dev.project.booking.dto.enums.PaymentConfirmationOutcome;
import dev.project.booking.dto.feign.ValidatedSeatsResponse;
import dev.project.booking.integration.kafka.service.BookingOutboxService;
import dev.project.booking.repository.entity.Booking;
import dev.project.booking.repository.entity.BookingSeat;
import dev.project.booking.repository.entity.RedisCleanupTask;
import dev.project.booking.repository.entity.SeatReservation;
import dev.project.booking.repository.entity.enums.BookingSeatStatus;
import dev.project.booking.repository.entity.enums.BookingStatus;
import dev.project.booking.repository.entity.enums.SeatReservationStatus;
import dev.project.booking.repository.postgresql.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingPersistenceService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final BookingOutboxService bookingOutboxService;
    private final BookingIdempotencyService bookingIdempotencyService;
    private final EventLockService eventLockService;
    private final CancelledEventsRepository cancelledEventsRepository;
    private final RedisCleanupRepository redisCleanupRepository;


    @Transactional
    public BookingResponse create(
            UUID bookingId,
            CreateBookingRequest request,
            ValidatedSeatsResponse validated,
            String guestTokenHash,
            LocalDateTime expiresAt,
            String idempotencyKey,
            String requestHash
    ) {
        eventLockService.lock(request.eventId());

        if (cancelledEventsRepository.existsById(request.eventId())) {
            throw new BusinessConflictException("Event already cancelled");
        }


        if (!expiresAt.isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessConflictException("Booking has expired");
        }

        Booking booking = Booking.builder()
                .id(bookingId)
                .eventId(request.eventId())
                .customerEmail(request.customerEmail())
                .customerPhone(request.customerPhone())
                .expiresAt(expiresAt)
                .status(BookingStatus.HOLD)
                .guestTokenHash(guestTokenHash)
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

        var response = toResponse(savedBooking, bookingSeats);
        bookingIdempotencyService.saveResult(guestTokenHash, idempotencyKey, requestHash, response);

        return response;
    }


    @Transactional(readOnly = true)
    public BookingResponse findById(UUID bookingId) {
        Booking booking = getBooking(bookingId);

        List<BookingSeat> seats = bookingSeatRepository.findAllByBooking_Id(bookingId);

        return toResponse(booking, seats);
    }


    @Transactional
    public BookingData cancel(UUID bookingId) {
        Booking booking = getBookingForUpdate(bookingId);

        switch (booking.getStatus()) {
            case BookingStatus.PAYMENT_FAILED -> {
                throw new BusinessConflictException(
                        "Booking payment failed status cannot be cancelled"
                );
            }

            case BookingStatus.EXPIRED -> {
                throw new BusinessConflictException(
                        "Booking has already expired"
                );
            }

            case BookingStatus.CONFIRMED -> {
                throw new BusinessConflictException(
                        "Booking has already confirmed"
                );
            }

            case BookingStatus.PARTIALLY_CANCELLED -> {
                throw new BusinessConflictException(
                        "Booking has already partially cancelled"
                );
            }

            case HOLD -> {
                booking.setStatus(BookingStatus.CANCELLED);
                seatReservationRepository.deleteAllByBooking_Id(bookingId);
                bookingOutboxService.saveBookingCancelled(bookingId);
            }

            case CANCELLED -> {
            }

            default -> {
                throw new BusinessConflictException("Invalid BookingStatus");
            }


        }

        List<BookingSeat> seats =
                bookingSeatRepository.findAllByBooking_Id(bookingId);


        return new BookingData(
                booking.getId(),
                booking.getEventId(),
                seats.stream().map(BookingSeat::getSeatId).toList(),
                booking.getCustomerEmail()
        );


    }

    @Transactional
    public PaymentConfirmationResult confirmPayment(
            UUID bookingId,
            UUID paymentId,
            Instant paidAt,
            long amountMinor,
            String currency
    ) {
        if (paymentId == null || paidAt == null) {
            throw new IllegalArgumentException("paymentId or paidAt is null");
        }

        var booking = getBookingForUpdate(bookingId);

        if (Objects.equals(booking.getConfirmedPaymentId(), paymentId)) {
            return new PaymentConfirmationResult(
                    PaymentConfirmationOutcome.ALREADY_PROCESSED,
                    null,
                    null
            );
        } else if (booking.getConfirmedPaymentId() != null) {
            return new PaymentConfirmationResult(
                    PaymentConfirmationOutcome.REJECTED,
                    null,
                    "Booking already confirmed by another payment"
            );
        }

        if (booking.getStatus() != BookingStatus.HOLD) {
            return new PaymentConfirmationResult(
                    PaymentConfirmationOutcome.REJECTED,
                    null,
                    "Booking is no longer hold"
            );
        }

        if (!paidAt.isBefore(booking.getExpiresAt().toInstant(ZoneOffset.UTC))) {
            return new PaymentConfirmationResult(
                    PaymentConfirmationOutcome.REJECTED,
                    null,
                    "Payment occurred after booking expiration"
            );
        }

        List<BookingSeat> seats =
                bookingSeatRepository.findAllByBooking_Id(bookingId);

        List<SeatReservation> reservations =
                seatReservationRepository.findAllByBooking_Id(bookingId);


        if (reservations.size() != seats.size() || seats.isEmpty()) {
            return new PaymentConfirmationResult(
                    PaymentConfirmationOutcome.REJECTED,
                    null,
                    "Booking reservations are inconsistent"
            );
        }

        var seatIds = seats.stream()
                .map(BookingSeat::getSeatId)
                .collect(Collectors.toSet());
        var reservationSeatIds = reservations.stream()
                .map(SeatReservation::getSeatId)
                .collect(Collectors.toSet());
        if (!seatIds.equals(reservationSeatIds)) {
            return new PaymentConfirmationResult(
                    PaymentConfirmationOutcome.REJECTED,
                    null,
                    "Booking reservations are inconsistent"
            );
        }

        if (reservations.stream().anyMatch(
                seatReservation -> seatReservation.getStatus() != SeatReservationStatus.HOLD)) {
            return new PaymentConfirmationResult(
                    PaymentConfirmationOutcome.REJECTED,
                    null,
                    "Booking reservations are inconsistent"
            );
        }

        if (!"KZT".equals(currency)) {
            return new PaymentConfirmationResult(
                    PaymentConfirmationOutcome.REJECTED,
                    null,
                    "Payment currency mismatch"
            );
        }

        long expectedAmountMinor = seats.stream()
                .map(BookingSeat::getPriceAtBooking)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .movePointRight(2).longValueExact();

        if (amountMinor != expectedAmountMinor) {
            return new PaymentConfirmationResult(
                    PaymentConfirmationOutcome.REJECTED,
                    null,
                    "Payment amount mismatch"
            );
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedPaymentId(paymentId);

        reservations.forEach(reservation -> {
            reservation.setStatus(SeatReservationStatus.CONFIRMED);
            reservation.setExpiresAt(null);
        });

        return new PaymentConfirmationResult(
                PaymentConfirmationOutcome.CONFIRMED,
                new BookingData(
                        booking.getId(),
                        booking.getEventId(),
                        seats.stream().map(BookingSeat::getSeatId).toList(),
                        booking.getCustomerEmail()
                ),
                null
        );

    }


    @Transactional
    public BookingData failPayment(UUID bookingId) {
        Booking booking = getBookingForUpdate(bookingId);

        if (booking.getStatus() != BookingStatus.HOLD) {
            throw new BusinessConflictException(
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
                seats.stream().map(BookingSeat::getSeatId).toList(),
                booking.getCustomerEmail()
        );
    }


    @Transactional(readOnly = true)
    public BookingPaymentData getPaymentData(
            UUID bookingId
    ) {
        var booking = getBooking(bookingId);

        if (booking.getStatus() != BookingStatus.HOLD) {
            throw new BusinessConflictException("Booking status is not 'HOLD'");
        }

        if (!booking.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessConflictException("Booking is expired");
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


    @Transactional
    public void releaseSoldSeat(
            UUID bookingId,
            UUID eventId,
            UUID seatId
    ) {
        var booking = getBookingForUpdate(bookingId);

        if (!(BookingStatus.CONFIRMED.equals(booking.getStatus()) || BookingStatus.PARTIALLY_CANCELLED.equals(booking.getStatus()))) {
            throw new IllegalStateException("Booking status must be 'CONFIRMED' or 'PARTIALLY_CANCELLED'");
        }

        if (!Objects.equals(booking.getEventId(), eventId)) {
            throw new IllegalStateException("'eventId' must be same");
        }

        var reservation = seatReservationRepository.findByBooking_IdAndSeatId(
                bookingId,
                seatId
        ).orElseThrow(() -> new IllegalStateException("Confirmed seat reservation not found"));

        if (!SeatReservationStatus.CONFIRMED.equals(reservation.getStatus())) {
            throw new IllegalStateException(
                    "SeatReservation status must be 'CONFIRMED'"
            );
        }

        if (!Objects.equals(reservation.getEventId(), eventId)) {
            throw new IllegalStateException("'eventId' must be same");
        }

        seatReservationRepository.delete(reservation);
        seatReservationRepository.flush();

        var remainingReservations =
                seatReservationRepository.findAllByBooking_Id(bookingId);

        if (remainingReservations.isEmpty()) {
            booking.setStatus(BookingStatus.CANCELLED);
        } else {
            booking.setStatus(BookingStatus.PARTIALLY_CANCELLED);
        }

        var bookingSeat = bookingSeatRepository.findByBooking_IdAndSeatId(bookingId, seatId)
                .orElseThrow(() -> new EntityNotFoundException("Booking seat not found with seatId= " + seatId));

        if (bookingSeat.getStatus() != BookingSeatStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Booking seat status must be 'ACTIVE'"
            );
        }

        bookingSeat.setStatus(BookingSeatStatus.CANCELLED);

    }

    @Transactional
    public Optional<BookingData> cancelForCancelledEvent(UUID bookingId) {
        var booking = getBookingForUpdate(bookingId);

        if (!cancelledEventsRepository.existsById(booking.getEventId())) {
            return Optional.empty();
        }

        if (booking.getStatus() != BookingStatus.HOLD &&
                booking.getStatus() != BookingStatus.PARTIALLY_CANCELLED &&
                booking.getStatus() != BookingStatus.CONFIRMED) {
            return Optional.empty();
        }

        var seats = bookingSeatRepository.findAllByBooking_Id(bookingId);

        booking.setStatus(BookingStatus.CANCELLED);
        seats.forEach(r ->
                r.setStatus(BookingSeatStatus.CANCELLED)
        );

        seatReservationRepository.deleteAllByBooking_Id(bookingId);

        bookingOutboxService.saveBookingCancelled(bookingId);

        var now = Instant.now();
        RedisCleanupTask cleanupTask = RedisCleanupTask.builder()
                .bookingId(bookingId)
                .createdAt(now)
                .nextAttemptAt(now)
                .attempts(0)
                .build();
        redisCleanupRepository.save(cleanupTask);

        return Optional.of(
                new BookingData(
                        bookingId,
                        booking.getEventId(),
                        seats.stream()
                                .map(BookingSeat::getSeatId)
                                .toList(),
                        booking.getCustomerEmail()
                )
        );


    }


    private Booking getBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Booking not found with id=" + bookingId
                ));
    }

    private Booking getBookingForUpdate(UUID bookingId) {
        return bookingRepository.findByIdForUpdate(bookingId)
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
                        seat.getPriceAtBooking(),
                        seat.getStatus()
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
