package com.ums.authorization.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ums.authorization.dto.UserAuthorizationResponse;
import com.ums.authorization.service.AuthorizationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
public class InternalAuthorizationController {

	private final AuthorizationService authorizationService;

	@GetMapping("/{userId}/authorization")
	public UserAuthorizationResponse getUserAuthorization(
			@PathVariable UUID userId,
			@RequestParam(defaultValue = "PLATFORM") String scopeType,
			@RequestParam(defaultValue = "*") String scopeId) {

		return authorizationService.getUserAuthorization(userId, scopeType, scopeId);
	}

	@PostMapping("/{userId}/roles/default")
	public void assignDefaultRole(@PathVariable UUID userId) {

		authorizationService.assignDefaultRole(userId);
	}

}
