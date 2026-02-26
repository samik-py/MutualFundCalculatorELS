package com.mfcalculator.error;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

  private ApiErrorDetail toDetail(FieldError error) {
    return new ApiErrorDetail(error.getField(), error.getDefaultMessage());
  }
}
