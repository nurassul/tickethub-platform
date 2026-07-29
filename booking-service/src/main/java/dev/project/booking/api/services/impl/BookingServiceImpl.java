package dev.project.booking.api.services.impl;

import dev.project.booking.api.exceptions.SeatAlreadyReservedException;
import dev.project.booking.api.services.BookingPersistenceService;
import dev.project.booking.api.services.BookingService;
import dev.project.booking.dto.BookingData;
import dev.project.booking.dto.BookingResponse;
import dev.project.booking.dto.CreateBookingRequest;
import dev.project.booking.dto.feign.ValidateSeatsRequest;
import dev.project.booking.dto.feign.ValidatedSeatsResponse;
import dev.project.booking.feign.EventServiceClient;
import dev.project.booking.redis.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@RequiredArgsConstructor
@Service
public class BookingServiceImpl implements BookingService {

    private final EventServiceClient eventServiceClient;
    private final SeatHoldService seatHoldService;
    private final BookingPersistenceService persistenceService;


    @Override
    public BookingResponse createBooking(CreateBookingRequest request) {
        Set<UUID> uniqueSeatIds = new HashSet<>(request.seatIds());

        if (uniqueSeatIds.size() != request.seatIds().size()) {
            throw new IllegalArgumentException(
                    "Seat IDs must be unique"
            );
        }

        List<UUID> seatIds = List.copyOf(uniqueSeatIds);

        ValidatedSeatsResponse validated =
                eventServiceClient.validateSeats(
                        request.eventId(),
                        new ValidateSeatsRequest(seatIds)
                );

        UUID bookingId = UUID.randomUUID();

        boolean held = seatHoldService.tryHold(
                bookingId,
                request.eventId(),
                seatIds
        );

        if (!held) {
            throw new SeatAlreadyReservedException(
                    "One or more selected seats are already reserved"
            );
        }

        try {
            return persistenceService.create(
                    bookingId,
                    request,
                    validated
            );
        } catch (DataIntegrityViolationException exception) {
            seatHoldService.release(
                    bookingId,
                    request.eventId(),
                    seatIds
            );

            throw new SeatAlreadyReservedException(
                    "One or more selected seats are already reserved"
            );
        } catch (RuntimeException exception) {
            seatHoldService.release(
                    bookingId,
                    request.eventId(),
                    seatIds
            );

            throw exception;
        }

    }

    @Override
    public BookingResponse getBooking(UUID bookingId) {
        return persistenceService.findById(bookingId);
    }

    @Override
    public void cancelBooking(UUID bookingId) {
        BookingData data = persistenceService.cancel(bookingId);

        seatHoldService.release(
                data.bookingId(),
                data.eventId(),
                data.seatIds()
        );
    }

    @Override
    public void confirmBooking(UUID bookingId) {
        BookingData data = persistenceService.confirm(bookingId);

        seatHoldService.markSold(
                data.bookingId(),
                data.eventId(),
                data.seatIds()
        );
    }
}
