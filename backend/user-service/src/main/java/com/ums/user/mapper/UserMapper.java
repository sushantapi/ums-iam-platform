package com.ums.user.mapper;

import org.springframework.stereotype.Component;

import com.ums.user.dto.UserPreferenceResponse;
import com.ums.user.dto.UserProfileResponse;
import com.ums.user.entity.UserPreference;
import com.ums.user.entity.UserProfile;

@Component
public class UserMapper {

	/**
	 * UserProfile -> UserProfileResponse
	 */
	public UserProfileResponse mapToUserProfileResponse(UserProfile profile) {

		return UserProfileResponse.builder().userId(profile.getUserId()).firstName(profile.getFirstName())
				.lastName(profile.getLastName()).email(profile.getEmail()).mobile(profile.getMobile())
				.avatarUrl(profile.getAvatarUrl()).address(profile.getAddress()).city(profile.getCity())
				.state(profile.getState()).country(profile.getCountry()).zipCode(profile.getZipCode()).build();
	}

	/**
	 * UserPreference -> UserPreferenceResponse
	 */
	public UserPreferenceResponse mapToUserPreferenceResponse(UserPreference preference) {

		return UserPreferenceResponse.builder().userId(preference.getUserId()).language(preference.getLanguage())
				.theme(preference.getTheme()).emailNotification(preference.getEmailNotification())
				.smsNotification(preference.getSmsNotification()).build();
	}
}