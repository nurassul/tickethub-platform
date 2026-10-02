package dev.project.booking.api.services;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project.booking.api.exceptions.BusinessConflictException;
import dev.project.booking.dto.BookingResponse;
import dev.project.booking.repository.entity.BookingIdempotency;
import dev.project.booking.repository.postgresql.BookingIdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingIdempotencyService {

    private final BookingIdempotencyRepository bookingIdempotencyRepository;
    private final ObjectMapper objectMapper;


    @Transactional(readOnly = true)
    public Optional<BookingIdempotency> findExisting(String guestTokenHash, String idempotencyKey, String requestHash) {

        var bookingIdempotency = bookingIdempotencyRepository.findByGuestTokenHashAndIdempotencyKey(
                guestTokenHash, idempotencyKey
        );
        if (bookingIdempotency.isEmpty()) {
            return Optional.empty();
        }

        if (!Objects.equals(requestHash, bookingIdempotency.get().getRequestHash())) {
            throw new BusinessConflictException("Idempotency key was already used with a different request");
        }

        return bookingIdempotency;


    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveResult(
            String guestTokenHash,
            String idempotencyKey,
            String requestHash,
            BookingResponse response
    ) {
        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error while serializing", e);
        }

        var idempotencyToSave = BookingIdempotency.builder()
                .guestTokenHash(guestTokenHash)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .bookingId(response.id())
                .responseStatus(201)
                .responseBody(responseJson)
                .build();

        bookingIdempotencyRepository.save(idempotencyToSave);
    }


    public BookingResponse restoreResponse(BookingIdempotency savedResult) {
        String responseJson = savedResult.getResponseBody();

        BookingResponse response;
        try {
            response = objectMapper.readValue(responseJson, BookingResponse.class);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize saved booking response", e);
        }

        return response;
    }


}
