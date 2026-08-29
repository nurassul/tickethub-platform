package dev.project.booking.integration.payment.exception;

public class PaymentConflictException extends RuntimeException {
    public PaymentConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
