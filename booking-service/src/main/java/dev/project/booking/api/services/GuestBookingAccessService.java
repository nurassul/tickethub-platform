package dev.project.booking.api.services;


import dev.project.booking.repository.postgresql.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuestBookingAccessService {

    private final BookingRepository bookingRepository;
    private final GuestBookingTokenService guestBookingTokenService;


    @Transactional(readOnly = true)
    public void requireAccess(UUID bookingId, String guestToken) {
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(this::accessDenied);

        if (!guestBookingTokenService.matches(
                guestToken,
                booking.getGuestTokenHash()
        )) {
            throw accessDenied();
        }
    }

    private ResponseStatusException accessDenied() {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Booking access denied"
        );
    }



}
