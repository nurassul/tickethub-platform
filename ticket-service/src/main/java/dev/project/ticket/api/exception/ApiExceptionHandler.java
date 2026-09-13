package dev.project.ticket.api.exception;


import dev.project.ticket.dto.ApiError;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(
            EntityNotFoundException ex
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        new ApiError(
                                404,
                                "Not Found",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        new ApiError(
                                400,
                                "Bad Request",
                                "Invalid value for " + ex.getName() + ": " + ex.getValue()
                        )
                );
    }
}
