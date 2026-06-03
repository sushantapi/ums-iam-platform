package com.ums.authorization.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApiResponse<T> {

	private boolean success;

	private String message;

	private T data;

	private LocalDateTime timestamp;
}