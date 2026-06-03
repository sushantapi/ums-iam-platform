package com.ums.auth.service;

import java.io.InputStream;
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

	@Value("${jwt.private-key-path}")
	private String privateKeyPath;

	@Value("${jwt.public-key-path}")
	private String publicKeyPath;

	@Value("${jwt.access-token-expiry-ms:900000}")
	private long accessTokenExpiryMs;

	@Value("${jwt.refresh-token-expiry-ms:604800000}")
	private long refreshTokenExpiryMs;

	@Value("${jwt.issuer:ums-iam-platform}")
	private String issuer;

	private PrivateKey privateKey;
	private PublicKey publicKey;

	@PostConstruct
	public void loadKeys() {

		try {

			log.info("Private Path = {}", privateKeyPath);
			log.info("Public Path  = {}", publicKeyPath);

			log.info("Private URL = {}", getClass().getClassLoader().getResource(privateKeyPath));

			log.info("Public URL = {}", getClass().getClassLoader().getResource(publicKeyPath));

			privateKey = loadPrivateKey(privateKeyPath);
			publicKey = loadPublicKey(publicKeyPath);

			log.info("JWT RSA keys loaded successfully");

		} catch (Exception e) {

			log.error("JWT initialization failed", e);

			throw new RuntimeException(e);
		}
	}

	public String generateAccessToken(String userId, String email, Set<String> roles) {

		return Jwts.builder().id(UUID.randomUUID().toString()).subject(userId).issuer(issuer).issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + accessTokenExpiryMs)).claim("email", email)
				.claim("roles", roles).claim("type", "ACCESS").signWith(privateKey, Jwts.SIG.RS256).compact();
	}

	public String generateRefreshToken(String userId) {

		return Jwts.builder().id(UUID.randomUUID().toString()).subject(userId).issuer(issuer).issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + refreshTokenExpiryMs)).claim("type", "REFRESH")
				.signWith(privateKey, Jwts.SIG.RS256).compact();
	}

	public Claims validateAndExtract(String token) {

		return Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();
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

	private PrivateKey loadPrivateKey(String path) throws Exception {

		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);

		if (inputStream == null) {
			throw new RuntimeException("Private key not found: " + path);
		}

		String key = new String(inputStream.readAllBytes());

		key = key.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s",
				"");

		byte[] decoded = Base64.getDecoder().decode(key);

		return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
	}

	private PublicKey loadPublicKey(String path) throws Exception {

		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);

		if (inputStream == null) {
			throw new RuntimeException("Public key not found: " + path);
		}

		String key = new String(inputStream.readAllBytes());

		key = key.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s",
				"");

		byte[] decoded = Base64.getDecoder().decode(key);

		return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
	}
}