package com.ums.notification.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {

		Map<String, Object> response = new HashMap<>();

		response.put("timestamp", LocalDateTime.now());
		response.put("message", ex.getMessage());
		response.put("status", HttpStatus.NOT_FOUND.value());

		return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {

		Map<String, String> errors = new HashMap<>();

		ex.getBindingResult().getFieldErrors()
				.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

		return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(TemplateNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleTemplateNotFound(TemplateNotFoundException ex) {

		Map<String, Object> response = new HashMap<>();

		response.put("timestamp", LocalDateTime.now());

		response.put("message", ex.getMessage());

		response.put("status", HttpStatus.NOT_FOUND.value());

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	@ExceptionHandler(TemplateAlreadyExistsException.class)
	public ResponseEntity<Map<String, Object>> handleTemplateAlreadyExists(TemplateAlreadyExistsException ex) {

		Map<String, Object> response = new HashMap<>();

		response.put("timestamp", LocalDateTime.now());

		response.put("message", ex.getMessage());

		response.put("status", HttpStatus.CONFLICT.value());

		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}

}