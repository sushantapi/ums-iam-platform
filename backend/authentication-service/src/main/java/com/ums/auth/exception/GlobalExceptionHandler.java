package com.ums.auth.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.ums.auth.dto.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException ex) {
		HttpStatus status = switch (ex.getErrorCode()) {
		case "EMAIL_EXISTS" -> HttpStatus.CONFLICT;
		case "ACCOUNT_LOCKED" -> HttpStatus.TOO_MANY_REQUESTS;
		case "ACCOUNT_SUSPENDED" -> HttpStatus.FORBIDDEN;
		default -> HttpStatus.UNAUTHORIZED;
		};
		return ResponseEntity.status(status).body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage)
				.collect(Collectors.joining(", "));
		return ResponseEntity.badRequest().body(ApiResponse.error(message, "VALIDATION_ERROR"));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
		return ResponseEntity.status(ex.getStatusCode())
				.body(ApiResponse.error(ex.getReason(), "REQUEST_FAILED"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
		log.error("Unhandled exception", ex);
		return ResponseEntity.internalServerError()
				.body(ApiResponse.error("An internal error occurred", "INTERNAL_ERROR"));
	}
}
