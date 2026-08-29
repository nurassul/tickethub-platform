package dev.project.booking.api.controllers;


import dev.project.booking.api.services.BookingService;
import dev.project.booking.dto.BookingResponse;
import dev.project.booking.dto.CreateBookingRequest;
import dev.project.booking.dto.PaymentResponse;
import dev.project.booking.dto.PaymentStartResponse;
import dev.project.booking.repository.entity.Booking;
import dev.project.booking.utils.BookingMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;
    

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bookingService.createBooking(request));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable UUID bookingId
    ) {
        return ResponseEntity.ok(
                bookingService.getBooking(bookingId)
        );
    }


    @PostMapping("/{bookingId}/payment")
    public ResponseEntity<PaymentStartResponse> startPayment(@PathVariable UUID bookingId) {
        var response = bookingService.startPayment(bookingId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{paymentId}/payment")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID paymentId) {
        var response = bookingService.getPayment(paymentId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable UUID bookingId
    ) {
        bookingService.cancelBooking(bookingId);

        return ResponseEntity
                .noContent()
                .build();
    }


    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<Void> confirmBooking(
            @PathVariable UUID bookingId
    ) {
        bookingService.confirmBooking(bookingId);

        return ResponseEntity
                .noContent()
                .build();
    }
}
