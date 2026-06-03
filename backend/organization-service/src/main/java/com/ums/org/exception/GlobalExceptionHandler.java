package com.ums.org.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex) {

		String message = ex.getBindingResult().getFieldError().getDefaultMessage();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(LocalDateTime.now(), message));
	}
}
