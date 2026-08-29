package dev.project.booking.integration.payment.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
