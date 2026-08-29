package dev.project.booking.integration.payment;


import dev.project.booking.integration.payment.exception.*;
import dev.project.contracts.payment.v1.CreatePaymentRequest;
import dev.project.contracts.payment.v1.GetPaymentRequest;
import dev.project.contracts.payment.v1.PaymentResponse;
import dev.project.contracts.payment.v1.PaymentServiceGrpc;
import io.grpc.StatusRuntimeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class PaymentGrpcClient {

    private final PaymentServiceGrpc.PaymentServiceBlockingStub stub;
    private final long deadlineSeconds;


    // удобный адаптер над generated кодом
    public PaymentGrpcClient(
            PaymentServiceGrpc.PaymentServiceBlockingStub stub,
            @Value("${clients.payment.grpc.deadline-seconds}") long deadlineSeconds) {
        this.stub = stub;
        this.deadlineSeconds = deadlineSeconds;
    }


    public PaymentDetails getPayment(UUID paymentId) {
        Objects.requireNonNull(
                paymentId,
                "paymentId must not be null"
        );
        GetPaymentRequest request = GetPaymentRequest.newBuilder()
                .setPaymentId(paymentId.toString())
                .build();

        try {
            var response = stub
                    .withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                    .getPayment(request);

            return toPaymentDetails(response);
        } catch (StatusRuntimeException e) {
            throw mapGrpcException(e);
        }

    }


    public PaymentDetails createPayment(
            CreatePaymentCommand command
    ) {
        Objects.requireNonNull(
                command,
                "command must not be null"
        );

        CreatePaymentRequest request = CreatePaymentRequest.newBuilder()
                .setBookingId(command.bookingId().toString())
                .setAmountMinor(command.amountMinor())
                .setCurrency(command.currency())
                .setBookingExpiresAtUnix(command.bookingExpiresAt().getEpochSecond())
                .setIdempotencyKey(command.idempotencyKey())
                .build();

        try {
            var response = stub
                    .withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                    .createPayment(request);

            return toPaymentDetails(response);
        } catch (StatusRuntimeException e) {
            throw mapGrpcException(e);
        }

    }

    private PaymentDetails toPaymentDetails(
            PaymentResponse response
    ) {
        return new PaymentDetails(
                UUID.fromString(response.getPaymentId()),
                UUID.fromString(response.getBookingId()),
                response.getAmountMinor(),
                response.getCurrency(),
                response.getStatus(),
                response.getPaymentUrl(),
                response.getFailureReason(),
                Instant.ofEpochSecond(response.getBookingExpiresAtUnix()),
                Instant.ofEpochSecond(response.getCreatedAtUnix()),
                Instant.ofEpochSecond(response.getUpdatedAtUnix()),
                response.hasPaidAtUnix()
                        ? Instant.ofEpochSecond(response.getPaidAtUnix())
                        : null
        );
    }


    private RuntimeException mapGrpcException(
            StatusRuntimeException exception
    ) {
        String description = exception
                .getStatus()
                .getDescription();

        String message = description != null
                ? description
                : "Payment service request failed";

        return switch (exception.getStatus().getCode()) {

            case NOT_FOUND -> new PaymentNotFoundException(message, exception);

            case ALREADY_EXISTS, FAILED_PRECONDITION -> new PaymentConflictException(
                    message, exception);

            case INVALID_ARGUMENT -> new IllegalArgumentException(
                    message,
                    exception
            );

            case UNAVAILABLE -> new PaymentServiceUnavailableException(
                    "Payment service is unavailable",
                    exception
            );

            case DEADLINE_EXCEEDED -> new PaymentServiceTimeoutException(
                    "Payment service request timed out",
                    exception
            );

            default -> new PaymentIntegrationException(
                    "Unexpected payment service error: "
                            + exception.getStatus().getCode(),
                    exception
            );
        };
    }
}
