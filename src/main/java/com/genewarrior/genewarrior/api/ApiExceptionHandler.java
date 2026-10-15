package com.genewarrior.genewarrior.api;

import com.genewarrior.genewarrior.api.dto.ApiErrorDto;
import com.genewarrior.genewarrior.service.ApiNotFoundException;
import com.genewarrior.genewarrior.service.ExternalToolUnavailableException;
import com.genewarrior.genewarrior.service.PayloadTooLargeException;
import com.genewarrior.genewarrior.service.JobAdmissionException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.genewarrior.genewarrior.api")
public class ApiExceptionHandler {
    @ExceptionHandler(JobAdmissionException.class)
    ResponseEntity<ApiErrorDto> admission(JobAdmissionException exception) {
        return error(HttpStatus.valueOf(exception.status()), exception.getMessage(), Map.of());
    }

    @ExceptionHandler(PayloadTooLargeException.class)
    ResponseEntity<ApiErrorDto> payloadTooLarge(PayloadTooLargeException exception) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorDto> invalidBody(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.put(error.getField(), error.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Validation failed", fields);
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    ResponseEntity<ApiErrorDto> badRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ApiNotFoundException.class)
    ResponseEntity<ApiErrorDto> notFound(ApiNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ExternalToolUnavailableException.class)
    ResponseEntity<ApiErrorDto> toolUnavailable(ExternalToolUnavailableException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), Map.of());
    }

    private ResponseEntity<ApiErrorDto> error(HttpStatus status, String message, Map<String, String> fields) {
        return ResponseEntity.status(status)
                .body(new ApiErrorDto(Instant.now(), status.value(), status.getReasonPhrase(), message, fields));
    }
}
