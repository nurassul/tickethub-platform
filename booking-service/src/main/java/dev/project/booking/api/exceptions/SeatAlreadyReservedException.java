package dev.project.booking.api.exceptions;

public class SeatAlreadyReservedException extends RuntimeException {
    public SeatAlreadyReservedException(String message) {
        super(message);
    }

    public SeatAlreadyReservedException(String message, Throwable cause) {
        super(message, cause);
    }
}
