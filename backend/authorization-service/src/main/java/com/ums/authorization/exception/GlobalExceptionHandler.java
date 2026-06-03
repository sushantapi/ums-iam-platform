package com.ums.authorization.exception;

import com.ums.authorization.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(RoleNotFoundException.class)
	public ResponseEntity<ApiResponse<Object>> handleRoleNotFoundException(RoleNotFoundException ex) {

		ApiResponse<Object> response = ApiResponse.builder().success(false).message(ex.getMessage()).data(null)
				.timestamp(LocalDateTime.now()).build();

		return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(UserRoleAlreadyExistsException.class)
	public ResponseEntity<ApiResponse<Object>> handleUserRoleAlreadyExistsException(UserRoleAlreadyExistsException ex) {

		ApiResponse<Object> response = ApiResponse.builder().success(false).message(ex.getMessage()).data(null)
				.timestamp(LocalDateTime.now()).build();

		return new ResponseEntity<>(response, HttpStatus.CONFLICT);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {

		ApiResponse<Object> response = ApiResponse.builder().success(false).message(ex.getMessage()).data(null)
				.timestamp(LocalDateTime.now()).build();

		return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}