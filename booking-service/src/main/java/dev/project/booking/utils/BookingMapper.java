package dev.project.booking.utils;


import dev.project.booking.dto.BookingResponse;
import dev.project.booking.dto.CreateBookingRequest;
import dev.project.booking.repository.entity.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {


    public Booking toEntity(CreateBookingRequest request) {
        return Booking.builder()
                .customerEmail(request.customerEmail())
                .customerPhone(request.customerPhone())
                .eventId(request.eventId())
                .build();
    }



}
