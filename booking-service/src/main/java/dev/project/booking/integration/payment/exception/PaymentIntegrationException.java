package dev.project.booking.integration.payment.exception;

public class PaymentIntegrationException extends RuntimeException {
    public PaymentIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
