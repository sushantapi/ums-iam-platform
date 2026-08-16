package com.ums.authorization.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@Profile("dev")
public class TestController {

	@GetMapping("/permission")
	@PreAuthorize("hasAuthority('USER_CREATE')")
	public String permissionTest() {
		return "USER_CREATE granted";
	}

	@GetMapping("/role")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public String roleTest() {
		return "SUPER_ADMIN granted";
	}
}
