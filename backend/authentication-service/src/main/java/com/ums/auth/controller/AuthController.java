package com.ums.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.auth.dto.ApiResponse;
import com.ums.auth.dto.ForgotPasswordRequest;
import com.ums.auth.dto.LoginRequest;
import com.ums.auth.dto.MfaChallengeVerifyRequest;
import com.ums.auth.dto.MfaRecoveryCodesResponse;
import com.ums.auth.dto.MfaStatusResponse;
import com.ums.auth.dto.MfaTotpConfirmRequest;
import com.ums.auth.dto.MfaTotpSetupResponse;
import com.ums.auth.dto.RefreshTokenRequest;
import com.ums.auth.dto.RegisterRequest;
import com.ums.auth.dto.ResetPasswordRequest;
import com.ums.auth.dto.TokenResponse;
import com.ums.auth.service.AuthService;
import com.ums.auth.service.MfaService;
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
	private final MfaService mfaService;

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest request,
			HttpServletRequest httpRequest) {
		TokenResponse tokens = authService.register(request, getClientIp(httpRequest));
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Registration successful", tokens));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest) {
		TokenResponse response = authService.login(request, getClientIp(httpRequest));
		String message = response.isMfaRequired() ? "MFA verification required" : "Login successful";
		return ResponseEntity.ok(ApiResponse.ok(message, response));
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

	@PostMapping("/mfa/challenge/verify")
	public ResponseEntity<ApiResponse<TokenResponse>> verifyMfaChallenge(
			@Valid @RequestBody MfaChallengeVerifyRequest request,
			HttpServletRequest httpRequest) {
		TokenResponse response = authService.verifyMfaChallenge(request, getClientIp(httpRequest));
		return ResponseEntity.ok(ApiResponse.ok("MFA verification successful", response));
	}

	@PostMapping("/mfa/totp/setup")
	public ResponseEntity<ApiResponse<MfaTotpSetupResponse>> setupTotp(
			Authentication authentication,
			HttpServletRequest request) {
		MfaTotpSetupResponse response = mfaService.setupTotp(userId(authentication), getClientIp(request));
		return ResponseEntity.ok(ApiResponse.ok("MFA setup started", response));
	}

	@PostMapping("/mfa/totp/confirm")
	public ResponseEntity<ApiResponse<MfaRecoveryCodesResponse>> confirmTotp(
			@Valid @RequestBody MfaTotpConfirmRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		MfaRecoveryCodesResponse response =
				mfaService.confirmTotp(userId(authentication), request, getClientIp(httpRequest));
		return ResponseEntity.ok(ApiResponse.ok("MFA enabled. Save the recovery codes now.", response));
	}

	@GetMapping("/mfa/status")
	public ResponseEntity<ApiResponse<MfaStatusResponse>> mfaStatus(Authentication authentication) {
		return ResponseEntity.ok(ApiResponse.ok("MFA status", mfaService.status(userId(authentication))));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, Authentication authentication) {
		authService.logout(request, userId(authentication));
		return ResponseEntity.ok(ApiResponse.ok("Logout successful", null));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		TokenResponse response = authService.refreshToken(request);
		return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", response));
	}

	private java.util.UUID userId(Authentication authentication) {
		return (java.util.UUID) authentication.getPrincipal();
	}

	private String getClientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
