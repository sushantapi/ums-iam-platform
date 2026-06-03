package com.ums.auth.controller;

import com.ums.auth.dto.ApiResponse;
import com.ums.auth.dto.LoginRequest;
import com.ums.auth.dto.RegisterRequest;
import com.ums.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register User
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {

        return authService.register(request);
    }

    /**
     * Login User
     */
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse login(
            @Valid @RequestBody LoginRequest request
    ) {

        return authService.login(request);
    }

    /**
     * Refresh Access Token
     */
    @PostMapping("/refresh-token")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse refreshToken(
            @RequestParam String refreshToken
    ) {

        return authService.refreshToken(refreshToken);
    }

    /**
     * Forgot Password
     */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse forgotPassword(
            @RequestParam String email
    ) {

        return authService.forgotPassword(email);
    }

    /**
     * Verify OTP
     */
    @PostMapping("/verify-otp")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse verifyOtp(
            @RequestParam String email,
            @RequestParam String otp
    ) {

        return authService.verifyOtp(email, otp);
    }

    /**
     * Logout User
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse logout(
            @RequestHeader("Authorization") String authHeader
    ) {

        String token = authHeader.substring(7);

        return authService.logout(token);
    }
}