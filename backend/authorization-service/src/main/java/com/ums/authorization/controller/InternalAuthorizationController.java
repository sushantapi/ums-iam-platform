package com.ums.authorization.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.authorization.dto.UserAuthorizationResponse;
import com.ums.authorization.service.AuthorizationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalAuthorizationController {

	private final AuthorizationService authorizationService;

	@GetMapping("/{userId}/authorization")
	public UserAuthorizationResponse getUserAuthorization(@PathVariable UUID userId) {

		return authorizationService.getUserAuthorization(userId);
	}

	@PostMapping("/{userId}/roles/default")
	public void assignDefaultRole(@PathVariable UUID userId) {

		authorizationService.assignDefaultRole(userId);
	}
}