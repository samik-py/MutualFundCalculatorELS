package com.mfcalculator.error;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    List<ApiErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
        .map(this::toDetail)
        .collect(Collectors.toList());
    ApiError error = new ApiError("VALIDATION_ERROR", "Invalid request", details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex) {
    List<ApiErrorDetail> details = ex.getConstraintViolations().stream()
        .map(violation -> new ApiErrorDetail(violation.getPropertyPath().toString(), violation.getMessage()))
        .collect(Collectors.toList());
    ApiError error = new ApiError("VALIDATION_ERROR", "Invalid request", details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleBadJson(HttpMessageNotReadableException ex) {
    ApiError error = new ApiError("INVALID_REQUEST", "Malformed JSON request", List.of());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex) {
    HttpStatus status = HttpStatus.valueOf(((HttpStatusCode) ex.getStatusCode()).value());
    String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
    ApiError error = new ApiError(status.name(), message, List.of());
    return ResponseEntity.status(ex.getStatusCode()).body(error);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
    ApiError error = new ApiError("FORBIDDEN", "Access denied", List.of());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  private ApiErrorDetail toDetail(FieldError error) {
    return new ApiErrorDetail(error.getField(), error.getDefaultMessage());
  }
}
