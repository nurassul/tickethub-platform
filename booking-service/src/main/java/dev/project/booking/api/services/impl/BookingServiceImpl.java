package dev.project.booking.api.services.impl;

import dev.project.booking.api.exceptions.BusinessConflictException;
import dev.project.booking.api.exceptions.SeatAlreadyReservedException;
import dev.project.booking.api.services.*;
import dev.project.booking.dto.*;
import dev.project.booking.dto.feign.ValidateSeatsRequest;
import dev.project.booking.dto.feign.ValidatedSeatsResponse;
import dev.project.booking.feign.EventServiceClient;
import dev.project.booking.integration.payment.CreatePaymentCommand;
import dev.project.booking.integration.payment.PaymentGrpcClient;
import dev.project.booking.redis.service.SeatHoldService;
import feign.FeignException;
import feign.RetryableException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
    private final BookingExpirationService bookingExpirationService;
    private final PaymentGrpcClient paymentGrpcClient;
    private final GuestBookingTokenService guestBookingTokenService;
    private final GuestBookingAccessService guestBookingAccessService;
    private final BookingRequestHashService bookingRequestHashService;
    private final BookingIdempotencyService bookingIdempotencyService;


    @Override
    public BookingResponse createBooking(CreateBookingRequest request, String guestToken, String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key must not be blank");
        }
        if (idempotencyKey.length() > 255) {
            throw new IllegalArgumentException("Idempotency-Key must not exceed 255 characters");
        }

        String guestTokenHash = guestBookingTokenService.hash(guestToken);
        Set<UUID> uniqueSeatIds = new HashSet<>(request.seatIds());

        if (uniqueSeatIds.size() != request.seatIds().size()) {
            throw new IllegalArgumentException(
                    "Seat IDs must be unique"
            );
        }

        List<UUID> seatIds = List.copyOf(uniqueSeatIds);

        String requestHash = bookingRequestHashService.hash(request);
        var existingResult = bookingIdempotencyService.findExisting(guestTokenHash, idempotencyKey, requestHash);
        if (existingResult.isPresent()) {
               return bookingIdempotencyService.restoreResponse(existingResult.get());
        }

        ValidatedSeatsResponse validated = validateSeats(request.eventId(), seatIds);

        UUID bookingId = UUID.randomUUID();

        LocalDateTime expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10).truncatedTo(ChronoUnit.MILLIS);
        if (!expiresAt.isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessConflictException("Booking has expired");
        }

        boolean held = seatHoldService.tryHold(
                bookingId,
                request.eventId(),
                seatIds,
                expiresAt
        );

        if (!held) {
            var savedResult = bookingIdempotencyService.findExisting(guestTokenHash, idempotencyKey, requestHash);
            if (savedResult.isPresent()) {
                return bookingIdempotencyService.restoreResponse(savedResult.get());
            }
            throw new SeatAlreadyReservedException(
                    "One or more selected seats are already reserved"
            );
        }

        try {
            return persistenceService.create(
                    bookingId,
                    request,
                    validated,
                    guestTokenHash,
                    expiresAt,
                    idempotencyKey,
                    requestHash
            );
        } catch (DataIntegrityViolationException exception) {
            seatHoldService.release(
                    bookingId,
                    request.eventId(),
                    seatIds
            );

            if (isUniqueConstraintViolation(exception, "uk_booking_idempotency_guest_key")) {
                var savedResult = bookingIdempotencyService.findExisting(guestTokenHash, idempotencyKey, requestHash);
                if (savedResult.isPresent()) {
                    return bookingIdempotencyService.restoreResponse(savedResult.get());
                } else {
                    throw exception;
                }
            }

            if (isUniqueConstraintViolation(exception, "uk_active_event_seat")) {
                var savedResult = bookingIdempotencyService.findExisting(guestTokenHash, idempotencyKey, requestHash);
                if (savedResult.isPresent()) {
                    return bookingIdempotencyService.restoreResponse(savedResult.get());
                }
                throw new SeatAlreadyReservedException(
                        "One or more selected seats are already reserved",
                        exception
                );
            }

            throw exception;
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
    public BookingResponse getBooking(UUID bookingId, String guestToken) {
        guestBookingAccessService.requireAccess(bookingId, guestToken);
        return persistenceService.findById(bookingId);
    }

    @Override
    public void cancelBooking(UUID bookingId, String guestToken) {
        guestBookingAccessService.requireAccess(bookingId, guestToken);
        BookingData data = persistenceService.cancel(bookingId);

        seatHoldService.release(
                data.bookingId(),
                data.eventId(),
                data.seatIds()
        );
    }


    @Override
    public PaymentStartResponse startPayment(UUID bookingId, String guestToken) {
        guestBookingAccessService.requireAccess(bookingId, guestToken);
        var paymentData = persistenceService.getPaymentData(bookingId);

        long amountMinor = paymentData
                .totalPrice()
                .movePointRight(2)
                .longValueExact();

        String idempotencyKey =
                "booking-payment:" + bookingId;

        Instant expiresAt = paymentData
                .expiresAt()
                .toInstant(ZoneOffset.UTC);

        var command = new CreatePaymentCommand(
                paymentData.bookingId(),
                amountMinor,
                "KZT",
                expiresAt,
                idempotencyKey
        );

        var response = paymentGrpcClient.createPayment(command);

        return new PaymentStartResponse(
                response.paymentId(),
                response.bookingId(),
                response.status(),
                response.paymentUrl(),
                response.bookingExpiresAt()
        );


    }

    @Override
    public PaymentResponse getPayment(UUID paymentId, String guestToken) {
        var paymentDetails = paymentGrpcClient.getPayment(paymentId);

        guestBookingAccessService.requireAccess(
                paymentDetails.bookingId(),
                guestToken
        );

        return new PaymentResponse(
                paymentDetails.paymentId(),
                paymentDetails.bookingId(),
                paymentDetails.status(),
                paymentDetails.paymentUrl(),
                paymentDetails.bookingExpiresAt()
        );
    }


    private ValidatedSeatsResponse validateSeats(
            UUID eventId,
            List<UUID> seatIds
    ) {
        try {
            return eventServiceClient.validateSeats(
                    eventId,
                    new ValidateSeatsRequest(seatIds)
            );
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException(
                    "Event not found: " + eventId,
                    e
            );
        } catch (FeignException.BadRequest e) {
            throw new IllegalArgumentException(
                    "The event or selected seats cannot be booked.",
                    e
            );
        } catch (FeignException.Conflict e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The event or selected seats conflict with the booking request.",
                    e
            );
        } catch (FeignException.ServiceUnavailable e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Event service is temporarily unavailable.",
                    e
            );
        } catch (FeignException.GatewayTimeout e) {
            throw new ResponseStatusException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "Event service response timed out.",
                    e
            );
        } catch (RetryableException e) {
            if (hasTimeoutCause(e)) {
                throw new ResponseStatusException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "Event service request timed out.",
                        e
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Event service is temporarily unavailable.",
                    e
            );
        } catch (FeignException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Event service request failed.",
                    e
            );
        }
    }

    private boolean hasTimeoutCause(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }



    private boolean isUniqueConstraintViolation(
            Throwable exception,
            String constraintName
    ) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof ConstraintViolationException violation
                    && "23505".equals(violation.getSQLState())
                    && constraintName.equals(violation.getConstraintName())) {
                return true;
            }

            current = current.getCause();

        }

        return false;
    }









}
