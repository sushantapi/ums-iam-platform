package com.ums.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.auth.dto.ApiResponse;
import com.ums.auth.dto.ForgotPasswordRequest;
import com.ums.auth.dto.LoginRequest;
import com.ums.auth.dto.RefreshTokenRequest;
import com.ums.auth.dto.RegisterRequest;
import com.ums.auth.dto.ResetPasswordRequest;
import com.ums.auth.dto.TokenResponse;
import com.ums.auth.service.AuthService;
import com.ums.auth.service.PasswordRecoveryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private static final String PASSWORD_RESET_REQUEST_MESSAGE =
			"If an account exists for that email, password reset instructions have been sent.";

	private final AuthService authService;
	private final PasswordRecoveryService passwordRecoveryService;

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest request,
			HttpServletRequest httpRequest) {
		TokenResponse tokens = authService.register(request, getClientIp(httpRequest));
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Registration successful", tokens));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest) {
		TokenResponse tokens = authService.login(request, getClientIp(httpRequest));
		return ResponseEntity.ok(ApiResponse.ok("Login successful", tokens));
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<ApiResponse<Void>> forgotPassword(
			@Valid @RequestBody ForgotPasswordRequest request,
			HttpServletRequest httpRequest) {
		passwordRecoveryService.requestPasswordReset(request, getClientIp(httpRequest));
		return ResponseEntity.ok(ApiResponse.ok(PASSWORD_RESET_REQUEST_MESSAGE, null));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<ApiResponse<Void>> resetPassword(
			@Valid @RequestBody ResetPasswordRequest request,
			HttpServletRequest httpRequest) {
		passwordRecoveryService.resetPassword(request, getClientIp(httpRequest));
		return ResponseEntity.ok(ApiResponse.ok("Password reset successful. Please sign in again.", null));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, Authentication authentication) {
		authService.logout(request, (java.util.UUID) authentication.getPrincipal());
		return ResponseEntity.ok(ApiResponse.ok("Logout successful", null));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		TokenResponse response = authService.refreshToken(request);
		return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", response));
	}

	private String getClientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
