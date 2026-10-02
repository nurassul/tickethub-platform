package dev.project.booking.api.services;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project.booking.dto.CreateBookingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;


@Service
@RequiredArgsConstructor
public class BookingRequestHashService {

    private final ObjectMapper objectMapper;

    public String hash(CreateBookingRequest request)  {
        var seatIds = request.seatIds().stream()
                .sorted()
                .toList();

        CreateBookingRequest createBookingRequest = new CreateBookingRequest(
                request.eventId(),
                request.customerEmail(),
                request.customerPhone(),
                seatIds
        );

        try {
            byte[] requestBytes = objectMapper.writeValueAsBytes(createBookingRequest);
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(messageDigest.digest(requestBytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to hash booking request", e);
        }
    }


}
