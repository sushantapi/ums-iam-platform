package com.ums.user.controller;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.ums.user.dto.UserResponse;
import com.ums.user.dto.UserSummaryResponse;
import com.ums.user.dto.UserSummaryPageResponse;
import com.ums.user.entity.UserProfile;
import com.ums.user.exception.UserNotFoundException;
import com.ums.user.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

	private final UserProfileRepository repository;

	@GetMapping
	public UserSummaryPageResponse getUsers(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String search) {
		if (page < 0 || page > 100_000 || size < 1 || size > 200) {
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid page or size");
		}
		if (search != null && search.length() > 255) {
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.BAD_REQUEST, "search must not exceed 255 characters");
		}

		String query = search == null ? "" : escapeSearch(search.trim().toLowerCase(java.util.Locale.ROOT));
		var pageable = PageRequest.of(page, size, Sort.by("email").ascending());
		var users = query.isBlank() ? repository.findAll(pageable) : repository.searchSummaries(query, pageable);
		/*
		 * return UserSummaryPageResponse.from(users.map(user ->
		 * UserSummaryResponse.builder().id(user.getUserId())
		 * .email(user.getEmail()).firstName(user.getFirstName()).lastName(user.
		 * getLastName()).build()));
		 */
		return UserSummaryPageResponse.from(users.map(user -> new UserSummaryResponse(user.getUserId(),
				user.getFirstName(), user.getLastName(), user.getEmail())));
	}

	private String escapeSearch(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	@GetMapping("/{userId}")
	public UserResponse getUser(@PathVariable UUID userId) {

		UserProfile user = repository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));

		return new UserResponse(user.getUserId(), user.getEmail(), user.getFirstName(), user.getLastName());
	}
}
