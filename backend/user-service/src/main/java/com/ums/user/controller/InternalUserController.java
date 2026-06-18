package com.ums.user.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.user.dto.UserResponse;
import com.ums.user.entity.UserProfile;
import com.ums.user.exception.UserNotFoundException;
import com.ums.user.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

	private final UserProfileRepository repository;

	@GetMapping("/{userId}")
	public UserResponse getUser(@PathVariable UUID userId) {

		UserProfile user = repository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));

		return new UserResponse(user.getUserId(), user.getEmail(), user.getFirstName(), user.getLastName());
	}
}