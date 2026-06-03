package com.ums.authorization.exception;

public class UserRoleAlreadyExistsException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public UserRoleAlreadyExistsException(String message) {
        super(message);
    }
}