package dev.project.booking.api.services;

import dev.project.booking.api.exceptions.BusinessConflictException;
import dev.project.booking.integration.kafka.service.BookingOutboxService;
import dev.project.booking.redis.service.RedisTaskService;
import dev.project.booking.repository.entity.Booking;
import dev.project.booking.repository.entity.BookingSeat;
import dev.project.booking.repository.entity.enums.BookingSeatStatus;
import dev.project.booking.repository.entity.enums.BookingStatus;
import dev.project.booking.repository.entity.enums.RedisTaskOperation;
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














}
