package com.ums.user.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.user.UserRegisteredEvent;
import com.ums.user.dto.UpdateProfileRequest;
import com.ums.user.dto.UserProfileResponse;
import com.ums.user.entity.UserProfile;
import com.ums.user.exception.UserNotFoundException;
import com.ums.user.mapper.UserMapper;
import com.ums.user.repository.UserProfileRepository;
import com.ums.user.service.UserService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

	private final UserProfileRepository userProfileRepository;

	private final UserMapper userMapper;

//	private final UserRepository userRepository;

	/**
	 * Get Current Logged-In User
	 */
	@Override
	public UserProfileResponse getCurrentUser(UUID userId) {

		UserProfile profile = getUserProfileById(userId);

		return userMapper.mapToUserProfileResponse(profile);
	}

	/**
	 * Update User Profile
	 */
	@Override
	public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {

		UserProfile profile = getUserProfileById(userId);

		updateProfileFields(profile, request);

		UserProfile updatedProfile = userProfileRepository.save(profile);

		return userMapper.mapToUserProfileResponse(updatedProfile);
	}

	/**
	 * Fetch User Profile By Email
	 */
	private UserProfile getUserProfileById(UUID userId) {

		return userProfileRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException("User profile not found with id: " + userId));
	}

	/**
	 * Update Profile Fields
	 */
	private void updateProfileFields(UserProfile profile, UpdateProfileRequest request) {

		profile.setFirstName(request.getFirstName());
		profile.setLastName(request.getLastName());
		profile.setMobile(request.getMobile());
		profile.setAddress(request.getAddress());
		profile.setCity(request.getCity());
		profile.setState(request.getState());
		profile.setCountry(request.getCountry());
		profile.setZipCode(request.getZipCode());
	}

	/*
	 * @Override public void createUserProfile(CreateUserProfileRequest request) {
	 * 
	 * UserProfile profile =
	 * UserProfile.builder().firstName(request.getFirstName()).lastName(request.
	 * getLastName())
	 * .email(request.getEmail()).mobile(request.getMobile()).build();
	 * 
	 * userProfileRepository.save(profile); }
	 */

	@RabbitListener(queues = RabbitMQConstants.PROFILE_USER_REGISTERED_QUEUE)
	@Transactional
	public void createProfile(UserRegisteredEvent event) {

		Optional<UserProfile> existing = userProfileRepository.findByEmail(event.getEmail());

		if (existing.isPresent()) {
			log.info("Profile already exists for {}", event.getEmail());
			return;
		}

		UserProfile profile = UserProfile.builder().userId(event.getUserId()).email(event.getEmail())
				.firstName(event.getFirstName()).lastName(event.getLastName()).build();

		userProfileRepository.save(profile);
	}

	@Override
	public UserProfileResponse getUserById(UUID userId) {

		UserProfile profile = getUserProfileById(userId);

		return userMapper.mapToUserProfileResponse(profile);
	}

	@Override
	public void deleteProfile(UUID userId) {

		UserProfile profile = getUserProfileById(userId);

		userProfileRepository.delete(profile);
	}

	@Override
	public List<UserProfileResponse> getAllUsers() {

		return userProfileRepository.findAll().stream().map(profile -> userMapper.mapToUserProfileResponse(profile))
				.toList();
	}

}
