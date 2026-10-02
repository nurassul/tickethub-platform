package dev.project.booking.api.exceptions;


import dev.project.booking.api.exceptions.dto.ErrorResponseDTO;
import dev.project.booking.integration.payment.exception.PaymentConflictException;
import dev.project.booking.integration.payment.exception.PaymentNotFoundException;
import dev.project.booking.integration.payment.exception.PaymentServiceTimeoutException;
import dev.project.booking.integration.payment.exception.PaymentServiceUnavailableException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(SeatAlreadyReservedException.class)
    public ResponseEntity<ErrorResponseDTO> handleReservedSeat(
            SeatAlreadyReservedException exception
    ) {
        log.error("Hadle exception", exception);

        var errorResponseDTO = new ErrorResponseDTO(
                "Seat already reserved!",
                exception.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponseDTO);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception e) {
        log.error("Handle exception", e);

        var errorResponseDTO = new ErrorResponseDTO(
                "Internal server error",
                "An unexpected error occurred. Please try again later.",
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponseDTO);
    }

    @ExceptionHandler({
            EntityNotFoundException.class,
            NoSuchElementException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleEntityNotFound(RuntimeException e) {
        log.error("Handle EntityNotFound", e);

        var errorResponseDTO = new ErrorResponseDTO(
                "Entity not found",
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponseDTO);
    }

    @ExceptionHandler(exception = {
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(Exception e) {
        log.error("Handle badRequest", e);

        var errorResponseDTO = new ErrorResponseDTO(
                "Bad request",
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponseDTO);
    }

    @ExceptionHandler(
            exception = ResponseStatusException.class
    )
    public ResponseEntity<ErrorResponseDTO> handleResponseStatusException(
            ResponseStatusException responseStatusException
    ) {
        var errorResponseDTO = new ErrorResponseDTO(
                "Request rejected",
                responseStatusException.getReason(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(responseStatusException.getStatusCode())
                .body(errorResponseDTO);
    }

    @ExceptionHandler(
            exception = PaymentNotFoundException.class
    )
    public ResponseEntity<ErrorResponseDTO> handlePaymentNotFoundException(
            PaymentNotFoundException paymentNotFoundException
    ) {
        var errorResponseDTO = new ErrorResponseDTO(
                "Payment not found",
                "The requested payment does not exist.",
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponseDTO);
    }

    @ExceptionHandler(
            exception = PaymentConflictException.class
    )
    public ResponseEntity<ErrorResponseDTO> handlePaymentConflictException(
            PaymentConflictException paymentConflictException
    ) {
        var errorResponseDTO = new ErrorResponseDTO(
                "Payment conflict",
                "Operation is conflicting with payment status",
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponseDTO);
    }

    @ExceptionHandler(
            exception = PaymentServiceUnavailableException.class
    )
    public ResponseEntity<ErrorResponseDTO> handlePaymentServiceUnavailableException(
            PaymentServiceUnavailableException paymentServiceUnavailableException
    ) {
        var errorResponseDTO = new ErrorResponseDTO(
                "Payment service unavailable",
                "Payment service unavailable. Please try again later.",
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponseDTO);
    }

    @ExceptionHandler(
            exception = PaymentServiceTimeoutException.class
    )
    public ResponseEntity<ErrorResponseDTO> handlePaymentServiceTimeoutException(
            PaymentServiceTimeoutException paymentServiceTimeoutException
    ) {
        var errorResponseDTO = new ErrorResponseDTO(
                "Timeout exception",
                "Payment service response timed out.",
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(errorResponseDTO);
    }


    @ExceptionHandler(
            exception = HttpMessageNotReadableException.class
    )
    public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException httpMessageNotReadableException
    ) {
        var errorResponseDTO = new ErrorResponseDTO(
                "Bad request",
                "Request body is missing or contains invalid JSON or field values.",
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponseDTO);
    }

    @ExceptionHandler(
            exception = MethodArgumentTypeMismatchException.class
    )
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException methodArgumentTypeMismatchException
    ) {
        var errorResponseDTO = new ErrorResponseDTO(
                "Bad request",
                "Invalid format for parameter: " + methodArgumentTypeMismatchException.getName(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponseDTO);
    }

    @ExceptionHandler(
            exception = MethodArgumentNotValidException.class
    )
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException methodArgumentNotValidException
    ) {
        var errorResponseDTO = new ErrorResponseDTO(
                "Validation failed",
                buildValidationMessage(methodArgumentNotValidException),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponseDTO);

    }

    private String buildValidationMessage(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .collect(Collectors.joining("; "));

        return message.isBlank() ? "Invalid request data." : message;
    }


    @ExceptionHandler(
            exception = BusinessConflictException.class
    )
    public ResponseEntity<ErrorResponseDTO> handleBusinessConflictException(
            BusinessConflictException businessConflictException
    ) {
        var errorResponseDTO = new ErrorResponseDTO(
                "Business conflict",
                businessConflictException.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponseDTO);

    }










}
