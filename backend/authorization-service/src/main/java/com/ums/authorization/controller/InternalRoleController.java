package com.ums.authorization.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.authorization.dto.AssignRoleRequest;
import com.ums.authorization.service.AuthorizationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
public class InternalRoleController {

	private final AuthorizationService authorizationService;

	@PostMapping("/assign")
	public String assignRole(@Valid @RequestBody AssignRoleRequest request) {
		return authorizationService.assignRole(request);
	}
}
