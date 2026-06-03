package com.ums.auth.exception;

import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String errorCode;

	public AuthException(String message, String errorCode) {
		super(message);
		this.errorCode = errorCode;
	}
}