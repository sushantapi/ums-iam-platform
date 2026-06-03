package com.ums.notification.exception;

public class TemplateNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TemplateNotFoundException(String templateCode) {

		super("Template not found: " + templateCode);
	}
}