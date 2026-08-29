package dev.project.booking.integration.payment.exception;

public class PaymentServiceTimeoutException extends RuntimeException {
    public PaymentServiceTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
