package com.ums.auth.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Getter
public class JwtService {

	@Value("${jwt.private-key-path:}")
	private String privateKeyPath;

	@Value("${jwt.public-key-path:}")
	private String publicKeyPath;

	@Value("${jwt.private-key:}")
	private String privateKeyPem;

	@Value("${jwt.public-key:}")
	private String publicKeyPem;

	@Value("${jwt.access-token-expiry-ms:900000}")
	private long accessTokenExpiryMs;

	@Value("${jwt.refresh-token-expiry-ms:604800000}")
	private long refreshTokenExpiryMs;

	@Value("${security.mfa.challenge-expiry-ms:300000}")
	private long mfaChallengeExpiryMs;

	@Value("${jwt.issuer:ums-iam-platform}")
	private String issuer;

	@Value("${jwt.audience:ums-api-gateway}")
	private String audience;

	@Value("${jwt.key-id:local-dev-1}")
	private String keyId;

	private PrivateKey privateKey;
	private PublicKey publicKey;

	@PostConstruct
	public void loadKeys() {

		try {

			privateKey = loadPrivateKey(privateKeyPath, privateKeyPem);
			publicKey = loadPublicKey(publicKeyPath, publicKeyPem);

			log.info("JWT RSA keys loaded successfully");

		} catch (Exception e) {

			log.error("JWT initialization failed", e);

			throw new RuntimeException(e);
		}
	}

	/*
	 * public String generateAccessToken(String userId, String email, Set<String>
	 * roles) {
	 *
	 * return
	 * Jwts.builder().id(UUID.randomUUID().toString()).subject(userId).issuer(issuer
	 * ).issuedAt(new Date()) .expiration(new Date(System.currentTimeMillis() +
	 * accessTokenExpiryMs)).claim("email", email) .claim("roles",
	 * roles).claim("type", "ACCESS").signWith(privateKey,
	 * Jwts.SIG.RS256).compact(); }
	 */

	public String generateAccessToken(
			String userId,
			String email,
			Set<String> roles,
			Set<String> permissions,
			UUID sessionId) {
		return generateAccessToken(userId, email, roles, permissions, sessionId, null, false);
	}

	public String generateAccessToken(
			String userId,
			String email,
			Set<String> roles,
			Set<String> permissions,
			UUID sessionId,
			UUID organizationId,
			boolean mfaVerified) {

		var builder = Jwts.builder().header().keyId(keyId).and()
				.id(UUID.randomUUID().toString()).subject(userId).issuer(issuer).audience().add(audience).and().issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + accessTokenExpiryMs))
				.claim("email", email).claim("roles", roles).claim("permissions", permissions)
				.claim("sessionId", sessionId.toString()).claim("type", "ACCESS")
				.claim("mfaVerified", mfaVerified);
		if (organizationId != null) {
			builder.claim("organizationId", organizationId.toString());
		}
		return builder.signWith(privateKey, Jwts.SIG.RS256).compact();
	}

	public String generateRefreshToken(String userId, UUID sessionId) {

		return Jwts.builder().header().keyId(keyId).and()
				.id(UUID.randomUUID().toString()).subject(userId).issuer(issuer).audience().add(audience).and().issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + refreshTokenExpiryMs))
				.claim("sessionId", sessionId.toString()).claim("type", "REFRESH")
				.signWith(privateKey, Jwts.SIG.RS256).compact();
	}

	public String generateMfaChallengeToken(
			String userId,
			UUID organizationId,
			String client,
			String deviceInfo) {

		var builder = Jwts.builder().header().keyId(keyId).and()
				.id(UUID.randomUUID().toString()).subject(userId).issuer(issuer).audience().add(audience).and().issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + mfaChallengeExpiryMs))
				.claim("type", "MFA_CHALLENGE");
		if (organizationId != null) {
			builder.claim("organizationId", organizationId.toString());
		}
		if (StringUtils.hasText(client)) {
			builder.claim("client", client);
		}
		if (StringUtils.hasText(deviceInfo)) {
			builder.claim("deviceInfo", deviceInfo);
		}
		return builder.signWith(privateKey, Jwts.SIG.RS256).compact();
	}

	public Claims validateAndExtract(String token) {

		return Jwts.parser().verifyWith(publicKey).requireIssuer(issuer).requireAudience(audience).build()
				.parseSignedClaims(token).getPayload();
	}

	public boolean isTokenValid(String token) {

		try {
			validateAndExtract(token);
			return true;
		} catch (JwtException ex) {

			log.error("Invalid JWT token: {}", ex.getMessage());

			return false;
		}
	}

	public String extractJti(String token) {

		return validateAndExtract(token).getId();
	}

	private PrivateKey loadPrivateKey(String path, String inlinePem) throws Exception {

		String key = readKeyMaterial(path, inlinePem, "Private");
		key = key.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s",
				"");

		byte[] decoded = Base64.getDecoder().decode(key);

		return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
	}

	private PublicKey loadPublicKey(String path, String inlinePem) throws Exception {

		String key = readKeyMaterial(path, inlinePem, "Public");
		key = key.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s",
				"");

		byte[] decoded = Base64.getDecoder().decode(key);

		return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
	}

	private String readKeyMaterial(String path, String inlinePem, String keyName) throws Exception {

		if (inlinePem != null && !inlinePem.isBlank()) {
			return inlinePem.replace("\\n", "\n");
		}

		if (path == null || path.isBlank()) {
			throw new IllegalStateException(keyName + " key is not configured");
		}

		if (path.startsWith("classpath:")) {
			return readClasspathKey(path.substring("classpath:".length()), keyName);
		}

		Path filePath = Path.of(path);
		if (Files.exists(filePath)) {
			return Files.readString(filePath, StandardCharsets.UTF_8);
		}

		return readClasspathKey(path, keyName);
	}

	private String readClasspathKey(String path, String keyName) throws Exception {

		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);

		if (inputStream == null) {
			throw new IllegalStateException(keyName + " key not found: " + path);
		}

		return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
	}
}
