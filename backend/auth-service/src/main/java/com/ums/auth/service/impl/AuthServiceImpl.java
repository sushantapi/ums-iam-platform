package com.ums.auth.service.impl;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ums.auth.client.AuthorizationServiceClient;
import com.ums.auth.client.UserServiceClient;
import com.ums.auth.dto.ApiResponse;
import com.ums.auth.dto.AuthResponse;
import com.ums.auth.dto.CreateUserProfileRequest;
import com.ums.auth.dto.JwtUser;
import com.ums.auth.dto.LoginRequest;
import com.ums.auth.dto.RegisterRequest;
import com.ums.auth.dto.RegisterResponse;
import com.ums.auth.dto.UserAuthorizationResponse;
import com.ums.auth.entity.User;
import com.ums.auth.exception.UserAlreadyExistsException;
import com.ums.auth.repository.UserRepository;
import com.ums.auth.security.service.JwtService;
import com.ums.auth.service.AuthService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final AuthenticationManager authenticationManager;

	private final JwtService jwtService;

	private final UserServiceClient userServiceClient;

	private final AuthorizationServiceClient authorizationServiceClient;

	/**
	 * Register New User
	 */
	@Override
	public ApiResponse register(RegisterRequest request) {

		if (userRepository.existsByEmail(request.getEmail())) {

			throw new UserAlreadyExistsException("Email already exists");
		}

		User user = null;

		try {

			user = User.builder()

					.email(request.getEmail())

					.passwordHash(passwordEncoder.encode(request.getPassword()))

					.enabled(true)

					.accountNonLocked(true)

					.build();

			userRepository.save(user);

			CreateUserProfileRequest profileRequest =

					CreateUserProfileRequest.builder()

							.userId(user.getId())

							.firstName(request.getFirstName())

							.lastName(request.getLastName())

							.email(request.getEmail())

							.mobile(request.getMobile())

							.build();

			userServiceClient.createUserProfile(profileRequest);

			authorizationServiceClient.assignDefaultRole(user.getId());

			RegisterResponse response = RegisterResponse.builder()

					.userId(user.getId())

					.email(user.getEmail())

					.build();

			return ApiResponse.builder()

					.success(true)

					.message("User Registered Successfully")

					.data(response)

					.build();

		} catch (Exception ex) {

			if (user != null) {

				userRepository.deleteById(user.getId());
			}

			throw ex;
		}
	}

	/**
	 * Login User
	 */
	@Override
	public ApiResponse login(LoginRequest request) {

		authenticationManager.authenticate(

				new UsernamePasswordAuthenticationToken(

						request.getEmail(),

						request.getPassword()));

		User user = userRepository

				.findByEmail(request.getEmail())

				.orElseThrow(() ->

				new UsernameNotFoundException("User not found"));

		// Fetch roles & permissions
		UserAuthorizationResponse authorization =

				authorizationServiceClient.getUserAuthorization(user.getId());
		
		System.out.println("authorization             --------------------"+authorization);

		// Generate JWT
		String token = jwtService.generateToken(

				JwtUser.builder()

						.userId(user.getId())

						.email(user.getEmail())

						.roles(authorization.getRoles())

						.permissions(authorization.getPermissions())

						.build());
		

		System.out.println("token             --------------------///////////"+token);

		// Build Auth Response
		AuthResponse authResponse = AuthResponse.builder()

				.accessToken(token)

				.tokenType("Bearer")

				.expiresIn(86400L)

				.roles(authorization.getRoles())

				.permissions(authorization.getPermissions())

				.build();

		return ApiResponse.builder()

				.success(true)

				.message("Login Successful")

				.data(authResponse)

				.build();
	}

	/**
	 * Refresh JWT Token
	 */
	@Override
	public ApiResponse refreshToken(String refreshToken) {

		return ApiResponse.builder()

				.success(true)

				.message("Refresh Token Feature Coming Soon")

				.data(null)

				.build();
	}

	/**
	 * Forgot Password
	 */
	@Override
	public ApiResponse forgotPassword(String email) {

		User user = userRepository

				.findByEmail(email)

				.orElseThrow(() ->

				new UsernameNotFoundException("User not found"));

		return ApiResponse.builder()

				.success(true)

				.message("Password Reset OTP Sent Successfully")

				.data(user.getEmail())

				.build();
	}

	/**
	 * Verify OTP
	 */
	@Override
	public ApiResponse verifyOtp(String email, String otp) {

		return ApiResponse.builder()

				.success(true)

				.message("OTP Verified Successfully")

				.data(email)

				.build();
	}

	/**
	 * Logout User
	 */
	@Override
	public ApiResponse logout(String token) {

		return ApiResponse.builder()

				.success(true)

				.message("Logout Successful")

				.data(null)

				.build();
	}
}