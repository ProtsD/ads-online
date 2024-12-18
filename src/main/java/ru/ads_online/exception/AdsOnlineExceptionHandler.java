package ru.ads_online.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.mime.MimeTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class AdsOnlineExceptionHandler {
    private static final String DEFAULT_ERROR_MESSAGE = "Unexpected error";

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException exception) {
        Map<String, Object> errorBody = Map.of(
                "status", exception.getStatusCode().value(),
                "message", exception.getReason() != null ? exception.getReason() : DEFAULT_ERROR_MESSAGE
        );

        log.error("Unexpected error", exception);
        return ResponseEntity.status(exception.getStatusCode()).body(errorBody);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        Map<String, Object> errorBody = Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "message", message
        );

        log.warn("Validation error: {}", message);
        return ResponseEntity.badRequest().body(errorBody);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestPartException(MissingServletRequestPartException exception) {
        String message = exception.getBody().getDetail();

        Map<String, Object> errorBody = Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "message", message != null ? message : DEFAULT_ERROR_MESSAGE
        );

        log.warn("Validation error: {}", message);
        return ResponseEntity.badRequest().body(errorBody);
    }
    @ExceptionHandler(MimeTypeException.class)
    public ResponseEntity<Map<String, Object>> handleMMimeTypeException(MimeTypeException exception) {
        String message = exception.getMessage();

        Map<String, Object> errorBody = Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "message", message != null ? message : DEFAULT_ERROR_MESSAGE
        );

        log.warn("Validation error: {}", message);
        return ResponseEntity.badRequest().body(errorBody);
    }
}
