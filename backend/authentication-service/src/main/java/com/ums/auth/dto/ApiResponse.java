package com.ums.auth.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
	private boolean success;
	private String message;
	private T data;
	private String errorCode;
	@Builder.Default
	private Instant timestamp = Instant.now();

	public static <T> ApiResponse<T> ok(String message, T data) {
		return ApiResponse.<T>builder().success(true).message(message).data(data).build();
	}

	public static <T> ApiResponse<T> error(String message, String errorCode) {
		return ApiResponse.<T>builder().success(false).message(message).errorCode(errorCode).build();
	}
}