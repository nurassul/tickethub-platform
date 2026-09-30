package dev.project.booking.api.exceptions;

public class BusinessConflictException extends RuntimeException {
    public BusinessConflictException(String message) {
        super(message);
    }
}
