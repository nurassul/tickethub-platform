package dev.project.ticket.api.service;


import dev.project.ticket.feign.BookingServiceClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GuestTicketAccessService {

    private final BookingServiceClient bookingServiceClient;

    private static final Pattern TOKEN_FORMAT =
            Pattern.compile("[0-9a-f]{64}");


    public void requireAccess(
            UUID bookingId,
            String token
    ) {
        if (token == null || !TOKEN_FORMAT.matcher(token).matches()) {
            throw accessDenied();
        }

        try {
            bookingServiceClient.checkAccess(bookingId, token);
        } catch (FeignException.Forbidden e) {
            throw accessDenied();
        } catch (FeignException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Booking access verification is temporarily unavailable"
            );
        }

    }

    private ResponseStatusException accessDenied() {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Booking access denied"
        );
    }

}
