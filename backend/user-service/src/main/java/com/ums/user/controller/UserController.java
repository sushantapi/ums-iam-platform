package com.ums.user.controller;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.user.dto.UpdateProfileRequest;
import com.ums.user.dto.UserProfileResponse;
import com.ums.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	/**
	 * Current Logged-In User
	 */
	@GetMapping("/me")
	public UserProfileResponse currentUser(Authentication authentication) {

		return userService.getCurrentUser(currentUserId(authentication));
	}

	/**
	 * Update User Profile
	 */
	@PutMapping("/profile")
	public UserProfileResponse updateProfile(Authentication authentication,
			@Valid @RequestBody UpdateProfileRequest request) {

		return userService.updateProfile(currentUserId(authentication), request);
	}

	/**
	 * Get User By ID
	 */
	@GetMapping("/{userId}")
	@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('USER_ADMIN') or hasAuthority('USER_READ')")
	public UserProfileResponse getUserById(@PathVariable UUID userId) {

		return userService.getUserById(userId);
	}

	/**
	 * Soft Delete Profile
	 */
	@DeleteMapping("/profile")
	public String deleteProfile(Authentication authentication) {

		userService.deleteProfile(currentUserId(authentication));

		return "Profile deleted successfully";
	}

	private UUID currentUserId(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AccessDeniedException("Authenticated user is required");
		}

		try {
			return UUID.fromString(authentication.getName());
		} catch (IllegalArgumentException ex) {
			throw new AccessDeniedException("Authenticated user id is invalid");
		}
	}
}
