package com.ums.user.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * User Not Found Exception
	 */
	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleUserNotFoundException(UserNotFoundException ex) {

		Map<String, Object> response = new HashMap<>();

		response.put("success", false);
		response.put("message", ex.getMessage());
		response.put("timestamp", LocalDateTime.now());

		return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}

	/**
	 * Validation Exception
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {

		Map<String, String> errors = new HashMap<>();

		ex.getBindingResult().getAllErrors().forEach(error -> {

			String fieldName = ((FieldError) error).getField();

			String errorMessage = error.getDefaultMessage();

			errors.put(fieldName, errorMessage);
		});

		Map<String, Object> response = new HashMap<>();

		response.put("success", false);
		response.put("message", "Validation Failed");
		response.put("errors", errors);
		response.put("timestamp", LocalDateTime.now());

		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Generic Exception
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleException(Exception ex) {

		Map<String, Object> response = new HashMap<>();

		response.put("success", false);
		response.put("message", ex.getMessage());
		response.put("timestamp", LocalDateTime.now());

		return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}