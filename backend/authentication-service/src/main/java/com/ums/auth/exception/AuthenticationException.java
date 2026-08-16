package com.ums.auth.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends UmsException {
    public AuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "AUTH_001");
    }
}
