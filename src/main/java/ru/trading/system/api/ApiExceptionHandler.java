package ru.trading.system.api;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() == null ? status.getReasonPhrase() : exception.getReason();
        return response(status, errorCode(message), message, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is malformed", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error", Map.of());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message, Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), code, message, Instant.now(), fieldErrors));
    }

    private String errorCode(String message) {
        return switch (message) {
            case "Email is already registered" -> "EMAIL_ALREADY_REGISTERED";
            case "Invalid email or password" -> "INVALID_CREDENTIALS";
            case "Instrument not found" -> "INSTRUMENT_NOT_FOUND";
            case "Instrument already exists" -> "INSTRUMENT_ALREADY_EXISTS";
            case "Order not found" -> "ORDER_NOT_FOUND";
            case "Position not found" -> "POSITION_NOT_FOUND";
            case "RUB account not found" -> "RUB_ACCOUNT_NOT_FOUND";
            case "Insufficient available balance" -> "INSUFFICIENT_BALANCE";
            case "Insufficient available shares" -> "INSUFFICIENT_SHARES";
            case "Quantity must be a multiple of lot size" -> "INVALID_LOT_QUANTITY";
            case "Price must be a multiple of minimum step" -> "INVALID_PRICE_STEP";
            case "Only RUB instruments are supported" -> "UNSUPPORTED_CURRENCY";
            case "Order is no longer active" -> "ORDER_NOT_ACTIVE";
            default -> "REQUEST_" + message.replaceAll("[^A-Za-z0-9]+", "_").toUpperCase();
        };
    }
}
