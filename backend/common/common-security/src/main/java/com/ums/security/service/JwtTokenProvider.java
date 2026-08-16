package com.ums.security.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class JwtTokenProvider {

	@Value("${jwt.public-key-path:}")
	private String publicKeyPath;

	@Value("${jwt.public-key:}")
	private String publicKeyPem;

	private RSAPublicKey publicKey;

	@PostConstruct
	public void init() throws Exception {

		String key = readPublicKeyMaterial().replace("-----BEGIN PUBLIC KEY-----", "")
				.replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");

		byte[] decoded = Base64.getDecoder().decode(key);

		publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
	}

	public RSAPublicKey getPublicKey() {
		return publicKey;
	}

	private String readPublicKeyMaterial() throws Exception {

		if (publicKeyPem != null && !publicKeyPem.isBlank()) {
			return publicKeyPem.replace("\\n", "\n");
		}

		if (publicKeyPath == null || publicKeyPath.isBlank()) {
			throw new IllegalStateException("JWT public key is not configured");
		}

		if (publicKeyPath.startsWith("classpath:")) {
			return readClasspathKey(publicKeyPath.substring("classpath:".length()));
		}

		Path path = Path.of(publicKeyPath);
		if (Files.exists(path)) {
			return Files.readString(path, StandardCharsets.UTF_8);
		}

		return readClasspathKey(publicKeyPath);
	}

	private String readClasspathKey(String path) throws Exception {

		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);

		if (inputStream == null) {
			throw new IllegalStateException("JWT public key not found: " + path);
		}

		return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
	}
}
