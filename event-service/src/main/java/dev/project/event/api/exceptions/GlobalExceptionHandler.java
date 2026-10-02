package dev.project.event.api.exceptions;


import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

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
            exception = MissingServletRequestParameterException.class
    )
    public ResponseEntity<ErrorResponseDTO> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException missingServletRequestParameterException
    ) {
        var errorResponseDTO = new ErrorResponseDTO(
                "Bad request",
                "Missing required parameter: " + missingServletRequestParameterException.getParameterName(),
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


    @ExceptionHandler(
            exception = DataIntegrityViolationException.class
    )
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolationException(
            DataIntegrityViolationException dataIntegrityViolationException
    ) {
        if (!isUniqueConstraintViolation(dataIntegrityViolationException, "uk_event_seat_position")) {
            return handleGenericException(dataIntegrityViolationException);
        }

        var errorResponseDTO = new ErrorResponseDTO(
                "Seat already exists",
                "A seat with this sector, row and number already exists for the event.",
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
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
