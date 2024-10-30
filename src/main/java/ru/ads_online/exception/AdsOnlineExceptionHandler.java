package ru.ads_online.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class AdsOnlineExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException exception) {
        Map<String, Object> errorBody = Map.of(
                "status", exception.getStatusCode().value(),
                "message", exception.getReason() != null ? exception.getReason() : "Unexpected error"
        );

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(errorBody);
    }
}