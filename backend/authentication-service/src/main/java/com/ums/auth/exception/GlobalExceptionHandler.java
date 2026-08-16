package com.ums.auth.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

import com.ums.auth.dto.ApiResponse;
import com.ums.auth.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException ex, HttpServletRequest request) {
		log.warn("Auth exception: {} - {}", ex.getErrorCode(), ex.getMessage());
		HttpStatus status = switch (ex.getErrorCode()) {
		case "EMAIL_EXISTS" -> HttpStatus.CONFLICT;
		case "ACCOUNT_LOCKED" -> HttpStatus.TOO_MANY_REQUESTS;
		case "ACCOUNT_SUSPENDED", "ACCOUNT_INACTIVE", "SESSION_REVOKED" -> HttpStatus.FORBIDDEN;
		default -> HttpStatus.UNAUTHORIZED;
		};
		return ResponseEntity.status(status).body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
	}

	@ExceptionHandler(UmsException.class)
	public ResponseEntity<ErrorResponse> handleUmsException(UmsException ex, HttpServletRequest request) {
		log.error("UMS Exception [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex);

		ErrorResponse error = ErrorResponse.builder()
				.errorCode(ex.getErrorCode())
				.message(ex.getMessage())
				.status(ex.getHttpStatus().value())
				.timestamp(LocalDateTime.now())
				.path(request.getRequestURI())
				.build();

		return new ResponseEntity<>(error, ex.getHttpStatus());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
		log.warn("Validation error occurred");
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return ResponseEntity.badRequest().body(ApiResponse.error(message, "VALIDATION_ERROR"));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
		log.warn("Response status exception: {}", ex.getReason());
		return ResponseEntity.status(ex.getStatusCode())
				.body(ApiResponse.error(ex.getReason(), "REQUEST_FAILED"));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Resource not found", "NOT_FOUND"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
		return ResponseEntity.internalServerError()
				.body(ApiResponse.error("An internal error occurred", "INTERNAL_ERROR"));
	}
}
