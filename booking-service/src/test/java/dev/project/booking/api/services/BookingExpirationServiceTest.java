package dev.project.booking.api.services;


import dev.project.booking.integration.kafka.service.BookingOutboxService;
import dev.project.booking.redis.service.RedisTaskService;
import dev.project.booking.repository.entity.Booking;
import dev.project.booking.repository.entity.BookingSeat;
import dev.project.booking.repository.entity.enums.BookingSeatStatus;
import dev.project.booking.repository.entity.enums.BookingStatus;
import dev.project.booking.repository.entity.enums.RedisTaskOperation;
import dev.project.booking.repository.postgresql.BookingRepository;
import dev.project.booking.repository.postgresql.BookingSeatRepository;
import dev.project.booking.repository.postgresql.SeatReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class BookingExpirationServiceTest {

    @InjectMocks
    private BookingExpirationService bookingExpirationService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    @Mock
    private SeatReservationRepository seatReservationRepository;

    @Mock
    private BookingOutboxService bookingOutboxService;

    @Mock
    private RedisTaskService redisTaskService;



    @Test
    public void shouldExpireHeldBookingAndReleaseReservations() {
        var bookingId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var seatId = UUID.randomUUID();
        var expiresAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5);

        var booking = Booking.builder()
                .id(bookingId)
                .eventId(eventId)
                .status(BookingStatus.HOLD)
                .customerEmail("test@mail.com")
                .expiresAt(expiresAt)
                .build();

        var bookingSeat = BookingSeat.builder()
                .booking(booking)
                .seatId(seatId)
                .status(BookingSeatStatus.ACTIVE)
                .priceAtBooking(BigDecimal.valueOf(1000))
                .build();

        when(bookingRepository.findExpiredBookingIds(eq(BookingStatus.HOLD), any(LocalDateTime.class), eq(PageRequest.of(0, 100))))
                .thenReturn(List.of(bookingId));
        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));
        when(bookingSeatRepository.findAllByBooking_Id(bookingId))
                .thenReturn(List.of(bookingSeat));

        var actualData = bookingExpirationService.expireBookings();

        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        assertEquals(1, actualData.size());

        assertEquals(bookingId, actualData.getFirst().bookingId());
        assertEquals(eventId, actualData.getFirst().eventId());
        assertEquals(List.of(seatId), actualData.getFirst().seatIds());
        assertEquals("test@mail.com", actualData.getFirst().customerEmail());

        verify(seatReservationRepository).deleteAllByBooking_Id(bookingId);
        verify(bookingOutboxService).saveBookingExpired(booking);
        verify(redisTaskService).enqueue(bookingId, RedisTaskOperation.RELEASE);
    }



    @Test
    public void shouldNotExpireBookingBeforeDeadline() {
        var bookingId = UUID.randomUUID();
        var expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(1);

        var booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.HOLD)
                .expiresAt(expiresAt)
                .build();

        when(bookingRepository.findExpiredBookingIds(eq(BookingStatus.HOLD), any(LocalDateTime.class), eq(PageRequest.of(0, 100))))
                .thenReturn(List.of(bookingId));
        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));

        var actualData = bookingExpirationService.expireBookings();

        assertTrue(actualData.isEmpty());

        assertEquals(BookingStatus.HOLD, booking.getStatus());

        verifyNoInteractions(bookingSeatRepository);
        verifyNoInteractions(seatReservationRepository);
        verifyNoInteractions(bookingOutboxService);
        verifyNoInteractions(redisTaskService);
    }


    @Test
    public void shouldNotExpireConfirmedBooking() {
        var bookingId = UUID.randomUUID();
        var expiresAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5);

        var booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CONFIRMED)
                .expiresAt(expiresAt)
                .build();

        when(bookingRepository.findExpiredBookingIds(eq(BookingStatus.HOLD), any(LocalDateTime.class), eq(PageRequest.of(0, 100))))
                .thenReturn(List.of(bookingId));
        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));

        var actualData = bookingExpirationService.expireBookings();

        assertTrue(actualData.isEmpty());

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());

        verifyNoInteractions(bookingSeatRepository);
        verifyNoInteractions(seatReservationRepository);
        verifyNoInteractions(bookingOutboxService);
        verifyNoInteractions(redisTaskService);
    }







}
