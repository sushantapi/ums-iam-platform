package com.ums.auth.service;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ums.auth.dto.LoginRequest;
import com.ums.auth.dto.RefreshTokenRequest;
import com.ums.auth.dto.RegisterRequest;
import com.ums.auth.dto.TokenResponse;
import com.ums.auth.entity.Role;
import com.ums.auth.entity.Session;
import com.ums.auth.entity.User;
import com.ums.auth.entity.User.UserStatus;
import com.ums.auth.exception.AuthException;
import com.ums.auth.repository.RoleRepository;
import com.ums.auth.repository.SessionRepository;
import com.ums.auth.repository.UserRepository;
import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.user.UserRegisteredEvent;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private static final int MAX_FAILED_ATTEMPTS = 5;
	private static final int LOCKOUT_MINUTES = 30;

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuditService auditService;
	private final SessionRepository sessionRepository;
	private final TokenBlacklistService blacklistService;

	private final RabbitTemplate rabbitTemplate;

	@Transactional
	public TokenResponse register(RegisterRequest request, String ipAddress) {

		String email = request.getEmail().trim().toLowerCase();

		if (userRepository.existsByEmail(email)) {
			throw new AuthException("Email already registered", "EMAIL_EXISTS");
		}

		Role employeeRole = roleRepository.findByName("EMPLOYEE")
				.orElseThrow(() -> new RuntimeException("Default role not found"));

		User user = User.builder().email(email).passwordHash(passwordEncoder.encode(request.getPassword()))
				.firstName(request.getFirstName().trim()).lastName(request.getLastName().trim())
				.status(UserStatus.ACTIVE).provider(request.getProvider() != null ? request.getProvider() : "LOCAL")
				.externalId(request.getExternalId()).build();

		user.getRoles().add(employeeRole);

		User savedUser = userRepository.save(user);

		UserRegisteredEvent event = UserRegisteredEvent.builder().userId(savedUser.getId()).email(savedUser.getEmail())
				.firstName(savedUser.getFirstName()).lastName(savedUser.getLastName()).build();

		rabbitTemplate.convertAndSend(RabbitMQConstants.USER_EXCHANGE, RabbitMQConstants.USER_REGISTERED_ROUTING_KEY,
				event);

		String refreshToken = jwtService.generateRefreshToken(savedUser.getId().toString());

		auditService.log(savedUser.getId(), "REGISTER", ipAddress, "SUCCESS");

		log.info("User registered successfully: {}", savedUser.getEmail());

		return buildTokenResponse(savedUser, refreshToken);
	}

	@Transactional
	public TokenResponse login(LoginRequest request, String ipAddress) {

		String email = request.getEmail().trim().toLowerCase();

		User user = userRepository.findByEmail(email).orElseThrow(() -> {

			auditService.logAnonymous("LOGIN_FAILED", ipAddress, email);

			return new AuthException("Invalid credentials", "INVALID_CREDENTIALS");
		});

		if (user.isLocked()) {
			throw new AuthException("Account is locked. Please try again later.", "ACCOUNT_LOCKED");
		}

		if (user.getStatus() == UserStatus.SUSPENDED) {
			throw new AuthException("Account suspended. Contact support.", "ACCOUNT_SUSPENDED");
		}

		if (user.getStatus() == UserStatus.DELETED) {
			throw new AuthException("Invalid credentials", "INVALID_CREDENTIALS");
		}

		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {

			handleFailedLogin(user, ipAddress);

			throw new AuthException("Invalid credentials", "INVALID_CREDENTIALS");
		}

		user.setFailedLoginAttempts(0);
		user.setLockedUntil(null);
		user.setLastLoginAt(Instant.now());

		String refreshToken = jwtService.generateRefreshToken(user.getId().toString());

		Session session = Session.builder().user(user).refreshTokenHash(hash(refreshToken)).ipAddress(ipAddress)
				.deviceInfo(request.getDeviceInfo())
				.expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiryMs())).build();

		sessionRepository.save(session);
		userRepository.save(user);

		auditService.log(user.getId(), "LOGIN_SUCCESS", ipAddress, "SUCCESS");

		log.info("Login successful: {}", email);

		return buildTokenResponse(user, refreshToken);
	}

	private String hash(String token) {

		return DigestUtils.sha256Hex(token);
	}

	private void handleFailedLogin(User user, String ipAddress) {

		int attempts = user.getFailedLoginAttempts() + 1;

		user.setFailedLoginAttempts(attempts);

		if (attempts >= MAX_FAILED_ATTEMPTS) {

			user.setLockedUntil(Instant.now().plusSeconds(LOCKOUT_MINUTES * 60L));

			log.warn("User account locked: {}", user.getEmail());
		}

		userRepository.save(user);

		auditService.log(user.getId(), "LOGIN_FAILED", ipAddress, "FAILED");
	}

	private TokenResponse buildTokenResponse(User user, String refreshToken) {

		Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());

		String accessToken = jwtService.generateAccessToken(user.getId().toString(), user.getEmail(), roles);

		return TokenResponse.builder().accessToken(accessToken).refreshToken(refreshToken).tokenType("Bearer")
				.expiresIn(jwtService.getAccessTokenExpiryMs() / 1000).userId(user.getId().toString())
				.email(user.getEmail()).build();
	}

	@Transactional
	public TokenResponse refreshToken(RefreshTokenRequest request) {

		Claims claims = jwtService.validateAndExtract(request.getRefreshToken());

		if (!"REFRESH".equals(claims.get("type", String.class))) {

			throw new AuthException("Invalid refresh token", "INVALID_REFRESH_TOKEN");
		}

		String tokenHash = hash(request.getRefreshToken());

		Session session = sessionRepository.findByRefreshTokenHash(tokenHash)
				.orElseThrow(() -> new AuthException("Session not found", "SESSION_NOT_FOUND"));

		if (session.isRevoked()) {

			throw new AuthException("Session revoked", "SESSION_REVOKED");
		}

		User user = session.getUser();

		String newRefreshToken = jwtService.generateRefreshToken(user.getId().toString());

		session.setRefreshTokenHash(hash(newRefreshToken));

		session.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiryMs()));

		sessionRepository.save(session);

		return buildTokenResponse(user, newRefreshToken);
	}

//	@Transactional
//	public void logout(HttpServletRequest request) {
//
//		String token = request.getHeader("Authorization").replace("Bearer ", "");
//
//		Claims claims = jwtService.validateAndExtract(token);
//
//		String jti = claims.getId();
//
//		long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
//
//		blacklistService.blacklist(jti, ttl / 1000);
//
//		auditService.log(null, "LOGOUT", request.getRemoteAddr(), "SUCCESS");
//	}

	@Transactional
	public void logout(HttpServletRequest request) {

		try {

			String token = extractToken(request);

			Claims claims = jwtService.validateAndExtract(token);

			String jti = claims.getId();

			long ttl = claims.getExpiration().getTime() / 1000 - System.currentTimeMillis() / 1000;

			blacklistService.blacklist(jti, ttl);

		} catch (Exception ex) {

			log.warn("Redis unavailable. Skipping blacklist.");
		}
	}

	private String extractToken(HttpServletRequest request) {

		String authHeader = request.getHeader("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			throw new RuntimeException("Authorization token missing");
		}

		return authHeader.substring(7);
	}
}
