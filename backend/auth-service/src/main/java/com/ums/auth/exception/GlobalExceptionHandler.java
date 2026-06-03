package com.ums.auth.exception;

import com.ums.auth.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(UserAlreadyExistsException.class)
	public ResponseEntity<ApiResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {

		ApiResponse response = ApiResponse.builder().success(false).message(ex.getMessage()).build();

		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}
}