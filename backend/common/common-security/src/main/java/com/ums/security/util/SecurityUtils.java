package com.ums.security.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ums.security.dto.JwtUser;

public final class SecurityUtils {

	private SecurityUtils() {
	}

	public static JwtUser getCurrentUser() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null) {
			return null;
		}

		return (JwtUser) authentication.getPrincipal();
	}
}