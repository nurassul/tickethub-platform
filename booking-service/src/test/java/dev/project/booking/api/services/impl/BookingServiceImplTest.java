package dev.project.booking.api.services.impl;


import dev.project.booking.api.exceptions.SeatAlreadyReservedException;
import dev.project.booking.api.services.*;
import dev.project.booking.dto.BookingResponse;
import dev.project.booking.dto.BookingSeatResponse;
import dev.project.booking.dto.CreateBookingRequest;
import dev.project.booking.dto.feign.ValidateSeatsRequest;
import dev.project.booking.dto.feign.ValidatedSeatResponse;
import dev.project.booking.dto.feign.ValidatedSeatsResponse;
import dev.project.booking.feign.EventServiceClient;
import dev.project.booking.integration.payment.PaymentGrpcClient;
import dev.project.booking.redis.service.SeatHoldService;
import dev.project.booking.repository.entity.BookingIdempotency;
import dev.project.booking.repository.entity.enums.BookingSeatStatus;
import dev.project.booking.repository.entity.enums.BookingStatus;
import feign.FeignException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceImplTest {


    @InjectMocks
    private BookingServiceImpl bookingServiceImpl;

    @Mock
    private EventServiceClient eventServiceClient;

    @Mock
    private SeatHoldService seatHoldService;

    @Mock
    private BookingPersistenceService persistenceService;

    @Mock
    private BookingExpirationService bookingExpirationService;

    @Mock
    private PaymentGrpcClient paymentGrpcClient;

    @Mock
    private GuestBookingTokenService guestBookingTokenService;

    @Mock
    private GuestBookingAccessService guestBookingAccessService;

    @Mock
    private BookingRequestHashService bookingRequestHashService;

    @Mock
    private BookingIdempotencyService bookingIdempotencyService;


    @Test
    public void shouldReturnSavedResponseWithoutCreatingBooking() {
        var eventId = UUID.randomUUID();
        var seatId = UUID.randomUUID();
        var bookingId = UUID.randomUUID();

        var guestToken = "a".repeat(64);
        var guestTokenHash = "guest-hash";
        var idempotencyKey = "request-123";
        var requestHash = "request-a";

        CreateBookingRequest request = new CreateBookingRequest(
                eventId,
                "test@gmail.com",
                null,
                List.of(seatId)
        );


        BookingIdempotency savedResult = BookingIdempotency.builder()
                .guestTokenHash(guestTokenHash)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .bookingId(bookingId)
                .build();

        BookingResponse expectedResponse = new BookingResponse(
                bookingId,
                "test@gmail.com",
                null,
                eventId,
                List.of(new BookingSeatResponse(seatId, BigDecimal.valueOf(1000), BookingSeatStatus.ACTIVE)),
                BookingStatus.HOLD,
                LocalDateTime.parse("2026-10-17T10:10:00"),
                BigDecimal.valueOf(1000)
        );

        when(guestBookingTokenService.hash(guestToken)).thenReturn(guestTokenHash);
        when(bookingRequestHashService.hash(request)).thenReturn(requestHash);
        when(bookingIdempotencyService.findExisting(guestTokenHash, idempotencyKey, requestHash))
                .thenReturn(Optional.of(savedResult));
        when(bookingIdempotencyService.restoreResponse(savedResult)).thenReturn(expectedResponse);

        var actualResponse = bookingServiceImpl.createBooking(
                request,
                guestToken
                , idempotencyKey
        );

        assertEquals(expectedResponse, actualResponse);

        verifyNoInteractions(eventServiceClient, seatHoldService, persistenceService);

    }

    @Test
    public void shouldNotCreateBookingWhenSeatIsAlreadyReserved() {
        var eventId = UUID.randomUUID();
        var seatId = UUID.randomUUID();

        var guestToken = "a".repeat(64);
        var guestTokenHash = "guest-hash";
        var idempotencyKey = "request-123";
        var requestHash = "request-a";

        CreateBookingRequest request = new CreateBookingRequest(
                eventId,
                "test@gmail.com",
                null,
                List.of(seatId)
        );

        ValidatedSeatsResponse seatsResponse = new ValidatedSeatsResponse(
                eventId,
                List.of(new ValidatedSeatResponse(seatId, BigDecimal.valueOf(1000)))
        );

        when(guestBookingTokenService.hash(guestToken)).thenReturn(guestTokenHash);
        when(bookingRequestHashService.hash(request)).thenReturn(requestHash);
        when(bookingIdempotencyService.findExisting(guestTokenHash, idempotencyKey, requestHash))
                .thenReturn(Optional.empty());
        when(eventServiceClient.validateSeats(eventId, new ValidateSeatsRequest(List.of(seatId))))
                .thenReturn(seatsResponse);
        when(seatHoldService.tryHold(any(UUID.class), eq(eventId), eq(List.of(seatId)), any(LocalDateTime.class)))
                .thenReturn(false);

        assertThrows(SeatAlreadyReservedException.class, () -> bookingServiceImpl.createBooking(request, guestToken, idempotencyKey));

        verifyNoInteractions(persistenceService);
    }


    @Test
    public void shouldReleaseSeatHoldWhenBookingPersistenceFails() {
        var eventId = UUID.randomUUID();
        var seatId = UUID.randomUUID();

        var guestToken = "a".repeat(64);
        var guestTokenHash = "guest-hash";
        var idempotencyKey = "request-123";
        var requestHash = "request-a";

        CreateBookingRequest request = new CreateBookingRequest(
                eventId,
                "test@gmail.com",
                null,
                List.of(seatId)
        );

        ValidatedSeatsResponse seatsResponse = new ValidatedSeatsResponse(
                eventId,
                List.of(new ValidatedSeatResponse(seatId, BigDecimal.valueOf(1000)))
        );

        var bookingIdCaptor = ArgumentCaptor.forClass(UUID.class);

        when(guestBookingTokenService.hash(guestToken)).thenReturn(guestTokenHash);
        when(bookingRequestHashService.hash(request)).thenReturn(requestHash);
        when(bookingIdempotencyService.findExisting(guestTokenHash, idempotencyKey, requestHash))
                .thenReturn(Optional.empty());
        when(eventServiceClient.validateSeats(eventId, new ValidateSeatsRequest(List.of(seatId))))
                .thenReturn(seatsResponse);
        when(seatHoldService.tryHold(any(UUID.class), eq(eventId), eq(List.of(seatId)), any(LocalDateTime.class)))
                .thenReturn(true);

        when(persistenceService.create(any(UUID.class), eq(request), eq(seatsResponse), eq(guestTokenHash), any(LocalDateTime.class), eq(idempotencyKey), eq(requestHash)))
                .thenThrow(new RuntimeException("Database unavailable"));

        assertThrows(RuntimeException.class,() -> bookingServiceImpl.createBooking(request, guestToken, idempotencyKey));

        verify(seatHoldService).tryHold(bookingIdCaptor.capture(), eq(eventId), eq(List.of(seatId)), any(LocalDateTime.class));

        verify(seatHoldService).release(eq(bookingIdCaptor.getValue()), eq(eventId), eq(List.of(seatId)));
    }



    @Test
    public void shouldCreateBookingWithoutReleasingSeatHold() {
        var eventId = UUID.randomUUID();
        var seatId = UUID.randomUUID();

        var guestToken = "a".repeat(64);
        var guestTokenHash = "guest-hash";
        var idempotencyKey = "request-123";
        var requestHash = "request-a";

        CreateBookingRequest request = new CreateBookingRequest(
                eventId,
                "test@gmail.com",
                null,
                List.of(seatId)
        );

        ValidatedSeatsResponse seatsResponse = new ValidatedSeatsResponse(
                eventId,
                List.of(new ValidatedSeatResponse(seatId, BigDecimal.valueOf(1000)))
        );

        UUID id = UUID.randomUUID();
        LocalDateTime time = LocalDateTime.parse("2026-10-13T10:00:00");
        BookingResponse expectedResponse = new BookingResponse(
                id,
                "test@gmail.com",
                null,
                eventId,
                List.of(new BookingSeatResponse(seatId, BigDecimal.valueOf(1000), BookingSeatStatus.ACTIVE)),
                BookingStatus.HOLD,
                time,
                BigDecimal.valueOf(1000)
        );

        when(guestBookingTokenService.hash(guestToken)).thenReturn(guestTokenHash);
        when(bookingRequestHashService.hash(request)).thenReturn(requestHash);
        when(bookingIdempotencyService.findExisting(guestTokenHash, idempotencyKey, requestHash))
                .thenReturn(Optional.empty());
        when(eventServiceClient.validateSeats(eventId, new ValidateSeatsRequest(List.of(seatId))))
                .thenReturn(seatsResponse);

        var bookingIdCaptor = ArgumentCaptor.forClass(UUID.class);
        var expiresAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        when(seatHoldService.tryHold(any(UUID.class), eq(eventId), eq(List.of(seatId)), any(LocalDateTime.class)))
                .thenReturn(true);

        when(persistenceService.create(any(UUID.class), eq(request), eq(seatsResponse), eq(guestTokenHash), any(LocalDateTime.class), eq(idempotencyKey), eq(requestHash)))
                .thenReturn(expectedResponse);

        var actualResponse = bookingServiceImpl.createBooking(request, guestToken, idempotencyKey);

        verify(seatHoldService).tryHold(bookingIdCaptor.capture(), eq(eventId), eq(List.of(seatId)), expiresAtCaptor.capture());

        verify(persistenceService).create(eq(bookingIdCaptor.getValue()), eq(request), eq(seatsResponse), eq(guestTokenHash), eq(expiresAtCaptor.getValue()), eq(idempotencyKey), eq(requestHash));

        assertEquals(expectedResponse, actualResponse);
        verify(seatHoldService, never()).release(any(UUID.class), any(UUID.class), anyList());
    }



    @Test
    public void shouldReturnSavedResponseWhenSeatHoldFailsAfterConcurrentRequest() {
        var eventId = UUID.randomUUID();
        var seatId = UUID.randomUUID();
        var bookingId = UUID.randomUUID();

        var guestToken = "a".repeat(64);
        var guestTokenHash = "guest-hash";
        var idempotencyKey = "request-123";
        var requestHash = "request-a";

        CreateBookingRequest request = new CreateBookingRequest(
                eventId,
                "test@gmail.com",
                null,
                List.of(seatId)
        );


        BookingIdempotency savedResult = BookingIdempotency.builder()
                .guestTokenHash(guestTokenHash)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .bookingId(bookingId)
                .build();

        BookingResponse expectedResponse = new BookingResponse(
                bookingId,
                "test@gmail.com",
                null,
                eventId,
                List.of(new BookingSeatResponse(seatId, BigDecimal.valueOf(1000), BookingSeatStatus.ACTIVE)),
                BookingStatus.HOLD,
                LocalDateTime.parse("2026-10-17T10:10:00"),
                BigDecimal.valueOf(1000)
        );


        ValidatedSeatsResponse seatsResponse = new ValidatedSeatsResponse(
                eventId,
                List.of(new ValidatedSeatResponse(seatId, BigDecimal.valueOf(1000)))
        );


        when(guestBookingTokenService.hash(guestToken)).thenReturn(guestTokenHash);
        when(bookingRequestHashService.hash(request)).thenReturn(requestHash);
        when(eventServiceClient.validateSeats(eventId, new ValidateSeatsRequest(List.of(seatId))))
                .thenReturn(seatsResponse);
        when(seatHoldService.tryHold(any(UUID.class), eq(eventId), eq(List.of(seatId)), any(LocalDateTime.class)))
                .thenReturn(false);
        when(bookingIdempotencyService.restoreResponse(savedResult)).thenReturn(expectedResponse);

        when(bookingIdempotencyService.findExisting(guestTokenHash, idempotencyKey, requestHash))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedResult));

        var actualResult = bookingServiceImpl.createBooking(request, guestToken, idempotencyKey);

        assertEquals(expectedResponse, actualResult);

        verify(bookingIdempotencyService, times(2)).findExisting(guestTokenHash, idempotencyKey, requestHash);

        verifyNoInteractions(persistenceService);
        verify(seatHoldService, never()).release(any(UUID.class), any(UUID.class), anyList());

    }


    @Test
    public void shouldNotHoldOrPersistBookingWhenSeatValidationFails() {
        var eventId = UUID.randomUUID();
        var seatId = UUID.randomUUID();

        var guestToken = "a".repeat(64);
        var guestTokenHash = "guest-hash";
        var idempotencyKey = "request-123";
        var requestHash = "request-a";

        var validationError = mock(FeignException.BadRequest.class);

        CreateBookingRequest request = new CreateBookingRequest(
                eventId,
                "test@gmail.com",
                null,
                List.of(seatId)
        );


        when(guestBookingTokenService.hash(guestToken)).thenReturn(guestTokenHash);
        when(bookingRequestHashService.hash(request)).thenReturn(requestHash);
        when(bookingIdempotencyService.findExisting(guestTokenHash, idempotencyKey, requestHash))
                .thenReturn(Optional.empty());
        when(eventServiceClient.validateSeats(eventId, new ValidateSeatsRequest(List.of(seatId))))
                .thenThrow(validationError);

        var actualException = assertThrows(IllegalArgumentException.class, () -> bookingServiceImpl.createBooking(request, guestToken, idempotencyKey));

        assertSame(validationError, actualException.getCause());

        verifyNoInteractions(seatHoldService);
        verifyNoInteractions(persistenceService);
    }


    @Test
    public void shouldReleaseHoldAndRejectBookingWhenSeatConstraintIsViolated() {
        var eventId = UUID.randomUUID();
        var seatId = UUID.randomUUID();

        var guestToken = "a".repeat(64);
        var guestTokenHash = "guest-hash";
        var idempotencyKey = "request-123";
        var requestHash = "request-a";

        CreateBookingRequest request = new CreateBookingRequest(
                eventId,
                "test@gmail.com",
                null,
                List.of(seatId)
        );

        when(guestBookingTokenService.hash(guestToken)).thenReturn(guestTokenHash);
        when(bookingRequestHashService.hash(request)).thenReturn(requestHash);
        when(bookingIdempotencyService.findExisting(guestTokenHash, idempotencyKey, requestHash))
                .thenReturn(Optional.empty());

        ValidatedSeatsResponse seatsResponse = new ValidatedSeatsResponse(
                eventId,
                List.of(new ValidatedSeatResponse(seatId, BigDecimal.valueOf(1000)))
        );

        when(eventServiceClient.validateSeats(eventId, new ValidateSeatsRequest(List.of(seatId))))
                .thenReturn(seatsResponse);
        when(seatHoldService.tryHold(any(UUID.class), eq(eventId), eq(List.of(seatId)), any(LocalDateTime.class)))
                .thenReturn(true);

        var constraintError = mock(ConstraintViolationException.class);

        when(constraintError.getSQLState()).thenReturn("23505");
        when(constraintError.getConstraintName()).thenReturn("uk_active_event_seat");

        var databaseError = new DataIntegrityViolationException("Seat conflict", constraintError);

        when(persistenceService.create(any(UUID.class), eq(request), eq(seatsResponse), eq(guestTokenHash), any(LocalDateTime.class), eq(idempotencyKey), eq(requestHash)))
                .thenThrow(databaseError);

       var actualException = assertThrows(SeatAlreadyReservedException.class,() -> bookingServiceImpl.createBooking(request, guestToken, idempotencyKey));

       assertSame(databaseError, actualException.getCause());

    }


















}
