package dev.project.booking.api.services;

import dev.project.booking.dto.BookingResponse;
import dev.project.booking.dto.CreateBookingRequest;
import dev.project.booking.dto.PaymentResponse;
import dev.project.booking.dto.PaymentStartResponse;

import java.util.UUID;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request);

    BookingResponse getBooking(UUID bookingId);

    void cancelBooking(UUID bookingId);

    void confirmBooking(UUID bookingId);

    PaymentStartResponse startPayment(UUID bookingId);

    PaymentResponse getPayment(UUID paymentId);

}
