package dev.project.booking.api.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project.booking.api.exceptions.BusinessConflictException;
import dev.project.booking.repository.entity.BookingIdempotency;
import dev.project.booking.repository.postgresql.BookingIdempotencyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class BookingIdempotencyServiceTest {


    @Mock
    private BookingIdempotencyRepository idempotencyRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BookingIdempotencyService idempotencyService;


    @Test
    public void shouldRejectSameKeyWithDifferentRequest() {
        var guestTokenHash = "guest-hash";
        var idempotencyKey = "request-123";
        var originalRequestHash = "request-a";
        var differentRequestHash = "request-b";

        BookingIdempotency bookingIdempotency = BookingIdempotency.builder()
                .guestTokenHash(guestTokenHash)
                .idempotencyKey(idempotencyKey)
                .requestHash(originalRequestHash)
                .build();

        when(idempotencyRepository.findByGuestTokenHashAndIdempotencyKey(guestTokenHash, idempotencyKey))
                .thenReturn(Optional.of(bookingIdempotency));

        assertThrows(BusinessConflictException.class, () -> idempotencyService.findExisting(guestTokenHash, idempotencyKey, differentRequestHash));
    }


    @Test
    public void shouldReturnExistingRecordForSameRequest() {
        var guestTokenHash = "guest-hash";
        var idempotencyKey = "request-123";
        var requestHash = "request-a";
        var bookingId = UUID.randomUUID();

        BookingIdempotency bookingIdempotency = BookingIdempotency.builder()
                .guestTokenHash(guestTokenHash)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .bookingId(bookingId)
                .build();

        when(idempotencyRepository.findByGuestTokenHashAndIdempotencyKey(guestTokenHash, idempotencyKey))
                .thenReturn(Optional.of(bookingIdempotency));

        var result = idempotencyService.findExisting(guestTokenHash, idempotencyKey, requestHash);

        assertTrue(result.isPresent());

        var saved = result.orElseThrow();

        assertEquals(bookingId, saved.getBookingId());
        assertEquals(requestHash, saved.getRequestHash());
    }



    @Test
    public void shouldReturnEmptyWhenKeyNotFound() {
        var guestTokenHash = "guest-hash";
        var idempotencyKey = "request-123";
        var requestHash = "request-a";


        when(idempotencyRepository.findByGuestTokenHashAndIdempotencyKey(guestTokenHash, idempotencyKey))
                .thenReturn(Optional.empty());

        var result = idempotencyService.findExisting(guestTokenHash, idempotencyKey, requestHash);

        assertTrue(result.isEmpty());
    }
















}
