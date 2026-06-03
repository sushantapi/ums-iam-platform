
package com.ums.user.service;

import java.util.List;
import java.util.UUID;

import com.ums.events.event.user.UserRegisteredEvent;
import com.ums.user.dto.UpdateProfileRequest;
import com.ums.user.dto.UserProfileResponse;

public interface UserService {

	UserProfileResponse getCurrentUser(String email);

	UserProfileResponse updateProfile(String email, UpdateProfileRequest request);

	public void createProfile(UserRegisteredEvent event);

	UserProfileResponse getUserById(UUID userId);

	void deleteProfile(String name);

	List<UserProfileResponse> getAllUsers();
}