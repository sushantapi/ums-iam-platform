package com.ums.auth.service;

import com.ums.auth.dto.ApiResponse;
import com.ums.auth.dto.LoginRequest;
import com.ums.auth.dto.RegisterRequest;

public interface AuthService {

	ApiResponse register(RegisterRequest request);

	ApiResponse login(LoginRequest request);

	ApiResponse refreshToken(String refreshToken);

	ApiResponse forgotPassword(String email);

	ApiResponse verifyOtp(String email, String otp);

	ApiResponse logout(String token);
}