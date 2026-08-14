package com.ums.authorization.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.ums.authorization.dto.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler({RoleNotFoundException.class, PermissionNotFoundException.class})
	public ResponseEntity<ApiResponse<Object>> handleNotFound(RuntimeException ex) {
		return response(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(UserRoleAlreadyExistsException.class)
	public ResponseEntity<ApiResponse<Object>> handleUserRoleAlreadyExistsException(UserRoleAlreadyExistsException ex) {
		return response(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<Object>> handleBadRequest(IllegalArgumentException ex) {
		return response(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return response(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiResponse<Object>> handleResponseStatus(ResponseStatusException ex) {
		return ResponseEntity.status(ex.getStatusCode()).body(body(ex.getReason()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
		log.error("Unhandled authorization-service exception", ex);
		return response(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred");
	}

	private ResponseEntity<ApiResponse<Object>> response(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(body(message));
	}

	private ApiResponse<Object> body(String message) {
		return ApiResponse.builder().success(false).message(message).data(null)
				.timestamp(LocalDateTime.now()).build();
	}
}
