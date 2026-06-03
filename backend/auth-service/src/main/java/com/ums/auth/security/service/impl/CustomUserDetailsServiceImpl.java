package com.ums.auth.security.service.impl;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ums.auth.entity.User;
import com.ums.auth.repository.UserRepository;
import com.ums.auth.security.CustomUserDetails;
import com.ums.auth.security.service.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl implements CustomUserDetailsService {

	private final UserRepository userRepository;  

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		return new CustomUserDetails(user);
	}
}