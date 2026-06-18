package com.ums.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.auth.dto.ApiResponse;
import com.ums.auth.dto.LoginRequest;
import com.ums.auth.dto.RefreshTokenRequest;
import com.ums.auth.dto.RegisterRequest;
import com.ums.auth.dto.TokenResponse;
import com.ums.auth.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
//@PreAuthorize("hasAnyRole('SUPER_ADMIN','ORG_ADMIN')")
public class AuthController {

	private final AuthService authService;

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

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {

		authService.logout(request);

		return ResponseEntity.ok(ApiResponse.ok("Logout successful", null));
	}

	private String getClientIp(HttpServletRequest request) {

		String forwardedFor = request.getHeader("X-Forwarded-For");

		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}

		return request.getRemoteAddr();
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<TokenResponse>> refresh(@RequestBody RefreshTokenRequest request) {

		TokenResponse response = authService.refreshToken(request);

		return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", response));
	}
}