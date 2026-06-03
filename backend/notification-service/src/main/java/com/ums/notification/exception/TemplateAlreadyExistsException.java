package com.ums.notification.exception;

public class TemplateAlreadyExistsException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TemplateAlreadyExistsException(String templateCode) {

		super("Template already exists: " + templateCode);
	}
}