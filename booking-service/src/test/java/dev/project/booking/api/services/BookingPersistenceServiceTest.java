package dev.project.booking.api.services;

import dev.project.booking.api.exceptions.BusinessConflictException;
import dev.project.booking.dto.enums.PaymentConfirmationOutcome;
import dev.project.booking.integration.kafka.service.BookingOutboxService;
import dev.project.booking.redis.service.RedisTaskService;
import dev.project.booking.repository.entity.Booking;
import dev.project.booking.repository.entity.BookingSeat;
import dev.project.booking.repository.entity.SeatReservation;
import dev.project.booking.repository.entity.enums.BookingSeatStatus;
import dev.project.booking.repository.entity.enums.BookingStatus;
import dev.project.booking.repository.entity.enums.RedisTaskOperation;
import dev.project.booking.repository.entity.enums.SeatReservationStatus;
import dev.project.booking.repository.postgresql.BookingRepository;
import dev.project.booking.repository.postgresql.BookingSeatRepository;
import dev.project.booking.repository.postgresql.CancelledEventsRepository;
import dev.project.booking.repository.postgresql.SeatReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class BookingPersistenceServiceTest {

    @InjectMocks
    private BookingPersistenceService bookingPersistenceService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    @Mock
    private SeatReservationRepository seatReservationRepository;

    @Mock
    private BookingOutboxService bookingOutboxService;

    @Mock
    private BookingIdempotencyService bookingIdempotencyService;

    @Mock
    private EventLockService eventLockService;

    @Mock
    private CancelledEventsRepository cancelledEventsRepository;

    @Mock
    private RedisTaskService redisTaskService;


    @Test
    public void shouldRejectPaymentDataWhenBookingExpired() {
        var bookingId = UUID.randomUUID();

        var booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.HOLD)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1))
                .build();

        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThrows(BusinessConflictException.class, () -> bookingPersistenceService.getPaymentData(bookingId));

        verifyNoInteractions(bookingSeatRepository);
    }


    @Test
    public void shouldReturnPaymentDataForActiveBooking() {
        var bookingId = UUID.randomUUID();
        var expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10);

        var booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.HOLD)
                .expiresAt(expiresAt)
                .build();

        var bookingSeatFirst = BookingSeat.builder()
                .booking(booking)
                .seatId(UUID.randomUUID())
                .status(BookingSeatStatus.ACTIVE)
                .priceAtBooking(BigDecimal.valueOf(1000))
                .build();

        var bookingSeatSecond = BookingSeat.builder()
                .booking(booking)
                .seatId(UUID.randomUUID())
                .status(BookingSeatStatus.ACTIVE)
                .priceAtBooking(BigDecimal.valueOf(2000))
                .build();

        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        when(bookingSeatRepository.findAllByBooking_Id(bookingId))
                .thenReturn(List.of(bookingSeatFirst, bookingSeatSecond));

        var actualData = bookingPersistenceService.getPaymentData(bookingId);

        assertEquals(bookingId, actualData.bookingId());
        assertEquals(BookingStatus.HOLD, actualData.status());
        assertEquals(expiresAt, actualData.expiresAt());
        assertEquals(BigDecimal.valueOf(3000), actualData.totalPrice());
    }


    @Test
    public void shouldRejectPaymentDataWhenBookingCancelled() {
        var bookingId = UUID.randomUUID();

        var booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CANCELLED)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10))
                .build();

        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThrows(BusinessConflictException.class, () -> bookingPersistenceService.getPaymentData(bookingId));

        verifyNoInteractions(bookingSeatRepository);

    }


    @Test
    public void shouldRejectPaymentDataWhenBookingHasNoSeats() {
        var bookingId = UUID.randomUUID();

        var booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.HOLD)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10))
                .build();

        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        when(bookingSeatRepository.findAllByBooking_Id(bookingId))
                .thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> bookingPersistenceService.getPaymentData(bookingId));
    }


    @Test
    public void shouldCancelHeldBooking() {
        var bookingId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var seatId = UUID.randomUUID();

        var booking = Booking.builder()
                .id(bookingId)
                .eventId(eventId)
                .status(BookingStatus.HOLD)
                .customerEmail("test@mail.com")
                .build();

        var bookingSeat = BookingSeat.builder()
                .booking(booking)
                .seatId(seatId)
                .status(BookingSeatStatus.ACTIVE)
                .priceAtBooking(BigDecimal.valueOf(1000))
                .build();

        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));
        when(bookingSeatRepository.findAllByBooking_Id(bookingId))
                .thenReturn(List.of(bookingSeat));

        var actualData = bookingPersistenceService.cancel(bookingId);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());

        assertEquals(bookingId, actualData.bookingId());
        assertEquals(eventId, actualData.eventId());
        assertEquals("test@mail.com", actualData.customerEmail());
        assertEquals(List.of(seatId), actualData.seatIds());

        verify(seatReservationRepository).deleteAllByBooking_Id(bookingId);
        verify(bookingOutboxService).saveBookingCancelled(bookingId);
        verify(redisTaskService).enqueue(bookingId, RedisTaskOperation.RELEASE);

    }


    @Test
    public void shouldNotRepeatCancellationWhenBookingAlreadyCancelled() {
        var bookingId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var seatId = UUID.randomUUID();

        var booking = Booking.builder()
                .id(bookingId)
                .eventId(eventId)
                .status(BookingStatus.CANCELLED)
                .customerEmail("test@mail.com")
                .build();

        var bookingSeat = BookingSeat.builder()
                .booking(booking)
                .seatId(seatId)
                .status(BookingSeatStatus.ACTIVE)
                .priceAtBooking(BigDecimal.valueOf(1000))
                .build();

        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));
        when(bookingSeatRepository.findAllByBooking_Id(bookingId))
                .thenReturn(List.of(bookingSeat));

        var actualData = bookingPersistenceService.cancel(bookingId);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());

        assertEquals(bookingId, actualData.bookingId());
        assertEquals(eventId, actualData.eventId());
        assertEquals("test@mail.com", actualData.customerEmail());
        assertEquals(List.of(seatId), actualData.seatIds());

        verifyNoInteractions(seatReservationRepository);
        verifyNoInteractions(bookingOutboxService);
        verifyNoInteractions(redisTaskService);
    }


    @Test
    public void shouldRejectCancellationWhenBookingConfirmed() {
        var bookingId = UUID.randomUUID();

        var booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));

        assertThrows(BusinessConflictException.class, () -> bookingPersistenceService.cancel(bookingId));

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());

        verifyNoInteractions(bookingSeatRepository);
        verifyNoInteractions(seatReservationRepository);
        verifyNoInteractions(bookingOutboxService);
        verifyNoInteractions(redisTaskService);
    }


    @Test
    public void shouldNotProcessSamePaymentTwice() {

        var bookingId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();

        var booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CONFIRMED)
                .confirmedPaymentId(paymentId)
                .build();

        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));

        var actualResult = bookingPersistenceService.confirmPayment(
                bookingId,
                paymentId,
                Instant.now(),
                100000L,
                "KZT"
        );

        assertEquals(PaymentConfirmationOutcome.ALREADY_PROCESSED, actualResult.outcome());
        assertNull(actualResult.bookingData());
        assertNull(actualResult.rejectionReason());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(paymentId, booking.getConfirmedPaymentId());

        verifyNoInteractions(bookingSeatRepository);
        verifyNoInteractions(seatReservationRepository);
        verifyNoInteractions(bookingOutboxService);
        verifyNoInteractions(redisTaskService);
    }



    @Test
    public void shouldRejectAnotherPaymentForConfirmedBooking() {
        var bookingId = UUID.randomUUID();
        var confirmedPaymentId = UUID.randomUUID();
        var incomingPaymentId = UUID.randomUUID();

        var booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CONFIRMED)
                .confirmedPaymentId(confirmedPaymentId)
                .build();

        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));

        var actualResult = bookingPersistenceService.confirmPayment(
                bookingId,
                incomingPaymentId,
                Instant.now(),
                100000L,
                "KZT"
        );

        assertEquals(PaymentConfirmationOutcome.REJECTED, actualResult.outcome());
        assertNull(actualResult.bookingData());
        assertEquals("Booking already confirmed by another payment",actualResult.rejectionReason());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(confirmedPaymentId, booking.getConfirmedPaymentId());

        verifyNoInteractions(bookingSeatRepository);
        verifyNoInteractions(seatReservationRepository);
        verifyNoInteractions(bookingOutboxService);
        verifyNoInteractions(redisTaskService);
    }



    @Test
    public void shouldRejectPaymentForCancelledBooking() {
        var bookingId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();

        var booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CANCELLED)
                .build();

        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));

        var actualResult = bookingPersistenceService.confirmPayment(
                bookingId,
                paymentId,
                Instant.now(),
                100000L,
                "KZT"
        );

        assertEquals(PaymentConfirmationOutcome.REJECTED, actualResult.outcome());
        assertNull(actualResult.bookingData());
        assertEquals("Booking is no longer hold",actualResult.rejectionReason());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertNull(booking.getConfirmedPaymentId());

        verifyNoInteractions(bookingSeatRepository);
        verifyNoInteractions(seatReservationRepository);
        verifyNoInteractions(bookingOutboxService);
        verifyNoInteractions(redisTaskService);
    }


    @Test
    public void shouldRejectPaymentMadeAfterBookingExpiration() {
        var bookingId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var expiresAt = LocalDateTime.parse("2026-11-01T12:00:00");

        var booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.HOLD)
                .expiresAt(expiresAt)
                .build();

        var paidAt = expiresAt.toInstant(ZoneOffset.UTC).plusSeconds(1);


        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));

        var actualResult = bookingPersistenceService.confirmPayment(
                bookingId,
                paymentId,
                paidAt,
                100000L,
                "KZT"
        );


        assertEquals(PaymentConfirmationOutcome.REJECTED, actualResult.outcome());
        assertNull(actualResult.bookingData());
        assertEquals("Payment occurred after booking expiration",actualResult.rejectionReason());
        assertEquals(BookingStatus.HOLD, booking.getStatus());
        assertNull(booking.getConfirmedPaymentId());

        verifyNoInteractions(bookingSeatRepository);
        verifyNoInteractions(seatReservationRepository);
        verifyNoInteractions(bookingOutboxService);
        verifyNoInteractions(redisTaskService);
    }
    


    @Test
    public void shouldRejectPaymentMadeExactlyAtBookingExpiration() {
        var bookingId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var expiresAt = LocalDateTime.parse("2026-11-01T12:00:00");

        var booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.HOLD)
                .expiresAt(expiresAt)
                .build();

        var paidAt = expiresAt.toInstant(ZoneOffset.UTC);


        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));

        var actualResult = bookingPersistenceService.confirmPayment(
                bookingId,
                paymentId,
                paidAt,
                100000L,
                "KZT"
        );


        assertEquals(PaymentConfirmationOutcome.REJECTED, actualResult.outcome());
        assertNull(actualResult.bookingData());
        assertEquals("Payment occurred after booking expiration",actualResult.rejectionReason());
        assertEquals(BookingStatus.HOLD, booking.getStatus());
        assertNull(booking.getConfirmedPaymentId());

        verifyNoInteractions(bookingSeatRepository);
        verifyNoInteractions(seatReservationRepository);
        verifyNoInteractions(bookingOutboxService);
        verifyNoInteractions(redisTaskService);
    }


    @Test
    public void shouldConfirmPaymentMadeBeforeBookingExpiration() {
        var bookingId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var seatId = UUID.randomUUID();
        var expiresAt = LocalDateTime.parse("2026-11-01T12:00:00");

        var booking = Booking.builder()
                .id(bookingId)
                .eventId(eventId)
                .status(BookingStatus.HOLD)
                .expiresAt(expiresAt)
                .customerEmail("test@mail.com")
                .build();

        var bookingSeat = BookingSeat.builder()
                .booking(booking)
                .seatId(seatId)
                .status(BookingSeatStatus.ACTIVE)
                .priceAtBooking(BigDecimal.valueOf(1000))
                .build();

        var reservation = SeatReservation.builder()
                .booking(booking)
                .eventId(eventId)
                .seatId(seatId)
                .status(SeatReservationStatus.HOLD)
                .expiresAt(expiresAt)
                .build();


        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));
        when(bookingSeatRepository.findAllByBooking_Id(bookingId))
                .thenReturn(List.of(bookingSeat));
        when(seatReservationRepository.findAllByBooking_Id(bookingId))
                .thenReturn(List.of(reservation));

        var paidAt = expiresAt.toInstant(ZoneOffset.UTC).minusSeconds(1);

        var actualResult = bookingPersistenceService.confirmPayment(
                bookingId,
                paymentId,
                paidAt,
                100000L,
                "KZT"
        );

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(paymentId, booking.getConfirmedPaymentId());
        assertEquals(SeatReservationStatus.CONFIRMED, reservation.getStatus());
        assertNull(reservation.getExpiresAt());

        assertEquals(PaymentConfirmationOutcome.CONFIRMED, actualResult.outcome());
        assertNull(actualResult.rejectionReason());
        assertNotNull(actualResult.bookingData());
        assertEquals(bookingId, actualResult.bookingData().bookingId());
        assertEquals(eventId, actualResult.bookingData().eventId());
        assertEquals("test@mail.com", actualResult.bookingData().customerEmail());
        assertEquals(List.of(seatId), actualResult.bookingData().seatIds());

        verify(redisTaskService).enqueue(bookingId, RedisTaskOperation.MARK_SOLD);


    }


    @Test
    public void shouldRejectPaymentWhenAmountDoesNotMatch() {
        var bookingId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var seatId = UUID.randomUUID();
        var expiresAt = LocalDateTime.parse("2026-11-01T12:00:00");

        var booking = Booking.builder()
                .id(bookingId)
                .eventId(eventId)
                .status(BookingStatus.HOLD)
                .expiresAt(expiresAt)
                .customerEmail("test@mail.com")
                .build();

        var bookingSeat = BookingSeat.builder()
                .booking(booking)
                .seatId(seatId)
                .status(BookingSeatStatus.ACTIVE)
                .priceAtBooking(BigDecimal.valueOf(1000))
                .build();

        var reservation = SeatReservation.builder()
                .booking(booking)
                .eventId(eventId)
                .seatId(seatId)
                .status(SeatReservationStatus.HOLD)
                .expiresAt(expiresAt)
                .build();


        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));
        when(bookingSeatRepository.findAllByBooking_Id(bookingId))
                .thenReturn(List.of(bookingSeat));
        when(seatReservationRepository.findAllByBooking_Id(bookingId))
                .thenReturn(List.of(reservation));

        var paidAt = expiresAt.toInstant(ZoneOffset.UTC).minusSeconds(1);

        var actualResult = bookingPersistenceService.confirmPayment(
                bookingId,
                paymentId,
                paidAt,
                99900L,
                "KZT"
        );


        assertEquals(PaymentConfirmationOutcome.REJECTED, actualResult.outcome());
        assertNull(actualResult.bookingData());
        assertEquals("Payment amount mismatch", actualResult.rejectionReason());

        assertEquals(BookingStatus.HOLD, booking.getStatus());
        assertNull(booking.getConfirmedPaymentId());
        assertEquals(SeatReservationStatus.HOLD, reservation.getStatus());
        assertEquals(expiresAt,reservation.getExpiresAt());

        verifyNoInteractions(redisTaskService);
        verifyNoInteractions(bookingOutboxService);
    }


    @Test
    public void shouldRejectPaymentWhenCurrencyDoesNotMatch() {
        var bookingId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var seatId = UUID.randomUUID();
        var expiresAt = LocalDateTime.parse("2026-11-01T12:00:00");

        var booking = Booking.builder()
                .id(bookingId)
                .eventId(eventId)
                .status(BookingStatus.HOLD)
                .expiresAt(expiresAt)
                .customerEmail("test@mail.com")
                .build();

        var bookingSeat = BookingSeat.builder()
                .booking(booking)
                .seatId(seatId)
                .status(BookingSeatStatus.ACTIVE)
                .priceAtBooking(BigDecimal.valueOf(1000))
                .build();

        var reservation = SeatReservation.builder()
                .booking(booking)
                .eventId(eventId)
                .seatId(seatId)
                .status(SeatReservationStatus.HOLD)
                .expiresAt(expiresAt)
                .build();


        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));
        when(bookingSeatRepository.findAllByBooking_Id(bookingId))
                .thenReturn(List.of(bookingSeat));
        when(seatReservationRepository.findAllByBooking_Id(bookingId))
                .thenReturn(List.of(reservation));

        var paidAt = expiresAt.toInstant(ZoneOffset.UTC).minusSeconds(1);

        var actualResult = bookingPersistenceService.confirmPayment(
                bookingId,
                paymentId,
                paidAt,
                100000L,
                "USD"
        );


        assertEquals(PaymentConfirmationOutcome.REJECTED, actualResult.outcome());
        assertNull(actualResult.bookingData());
        assertEquals("Payment currency mismatch", actualResult.rejectionReason());

        assertEquals(BookingStatus.HOLD, booking.getStatus());
        assertNull(booking.getConfirmedPaymentId());
        assertEquals(SeatReservationStatus.HOLD, reservation.getStatus());
        assertEquals(expiresAt,reservation.getExpiresAt());

        verifyNoInteractions(redisTaskService);
        verifyNoInteractions(bookingOutboxService);
    }


    @Test
    public void shouldRejectPaymentWhenReservationSeatDoesNotMatch() {
        var bookingId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var seatId = UUID.randomUUID();
        var otherSeatId = UUID.randomUUID();
        var expiresAt = LocalDateTime.parse("2026-11-01T12:00:00");

        var booking = Booking.builder()
                .id(bookingId)
                .eventId(eventId)
                .status(BookingStatus.HOLD)
                .expiresAt(expiresAt)
                .customerEmail("test@mail.com")
                .build();

        var bookingSeat = BookingSeat.builder()
                .booking(booking)
                .seatId(seatId)
                .status(BookingSeatStatus.ACTIVE)
                .priceAtBooking(BigDecimal.valueOf(1000))
                .build();

        var reservation = SeatReservation.builder()
                .booking(booking)
                .eventId(eventId)
                .seatId(otherSeatId)
                .status(SeatReservationStatus.HOLD)
                .expiresAt(expiresAt)
                .build();


        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));
        when(bookingSeatRepository.findAllByBooking_Id(bookingId))
                .thenReturn(List.of(bookingSeat));
        when(seatReservationRepository.findAllByBooking_Id(bookingId))
                .thenReturn(List.of(reservation));

        var paidAt = expiresAt.toInstant(ZoneOffset.UTC).minusSeconds(1);

        var actualResult = bookingPersistenceService.confirmPayment(
                bookingId,
                paymentId,
                paidAt,
                100000L,
                "KZT"
        );

        assertEquals(PaymentConfirmationOutcome.REJECTED, actualResult.outcome());
        assertNull(actualResult.bookingData());
        assertEquals("Booking reservations are inconsistent", actualResult.rejectionReason());

        assertEquals(BookingStatus.HOLD, booking.getStatus());
        assertNull(booking.getConfirmedPaymentId());
        assertEquals(SeatReservationStatus.HOLD, reservation.getStatus());
        assertEquals(expiresAt,reservation.getExpiresAt());

        verifyNoInteractions(redisTaskService);
        verifyNoInteractions(bookingOutboxService);

    }


    @Test
    public void shouldRejectPaymentWhenReservationIsMissing() {
        var bookingId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var seatId = UUID.randomUUID();
        var expiresAt = LocalDateTime.parse("2026-11-01T12:00:00");

        var booking = Booking.builder()
                .id(bookingId)
                .eventId(eventId)
                .status(BookingStatus.HOLD)
                .expiresAt(expiresAt)
                .customerEmail("test@mail.com")
                .build();

        var bookingSeat = BookingSeat.builder()
                .booking(booking)
                .seatId(seatId)
                .status(BookingSeatStatus.ACTIVE)
                .priceAtBooking(BigDecimal.valueOf(1000))
                .build();



        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));
        when(bookingSeatRepository.findAllByBooking_Id(bookingId))
                .thenReturn(List.of(bookingSeat));
        when(seatReservationRepository.findAllByBooking_Id(bookingId))
                .thenReturn(List.of());

        var paidAt = expiresAt.toInstant(ZoneOffset.UTC).minusSeconds(1);

        var actualResult = bookingPersistenceService.confirmPayment(
                bookingId,
                paymentId,
                paidAt,
                100000L,
                "KZT"
        );

        assertEquals(PaymentConfirmationOutcome.REJECTED, actualResult.outcome());
        assertNull(actualResult.bookingData());
        assertEquals("Booking reservations are inconsistent", actualResult.rejectionReason());

        assertEquals(BookingStatus.HOLD, booking.getStatus());
        assertNull(booking.getConfirmedPaymentId());


        verifyNoInteractions(redisTaskService);
        verifyNoInteractions(bookingOutboxService);
    }






}
