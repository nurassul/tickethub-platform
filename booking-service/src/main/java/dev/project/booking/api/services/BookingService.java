package dev.project.booking.api.services;

import dev.project.booking.dto.BookingResponse;
import dev.project.booking.dto.CreateBookingRequest;
import dev.project.booking.dto.PaymentResponse;
import dev.project.booking.dto.PaymentStartResponse;

import java.util.UUID;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request, String guestToken);

    BookingResponse getBooking(UUID bookingId, String guestToken);

    void cancelBooking(UUID bookingId, String guestToken);


    PaymentStartResponse startPayment(UUID bookingId, String guestToken);

    PaymentResponse getPayment(UUID paymentId, String guestToken);

}
