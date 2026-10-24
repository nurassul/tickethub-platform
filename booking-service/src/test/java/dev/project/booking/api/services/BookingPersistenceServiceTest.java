package dev.project.booking.api.services;

import dev.project.booking.api.exceptions.BusinessConflictException;
import dev.project.booking.integration.kafka.service.BookingOutboxService;
import dev.project.booking.redis.service.RedisTaskService;
import dev.project.booking.repository.entity.Booking;
import dev.project.booking.repository.entity.enums.BookingStatus;
import dev.project.booking.repository.postgresql.BookingRepository;
import dev.project.booking.repository.postgresql.BookingSeatRepository;
import dev.project.booking.repository.postgresql.CancelledEventsRepository;
import dev.project.booking.repository.postgresql.SeatReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
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











}
