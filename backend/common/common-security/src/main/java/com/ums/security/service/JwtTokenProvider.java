package com.ums.security.service;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class JwtTokenProvider {

	@Value("${jwt.public-key-path}")
	private String publicKeyPath;

	private RSAPublicKey publicKey;

	@PostConstruct
	public void init() throws Exception {

		InputStream is = getClass().getClassLoader().getResourceAsStream(publicKeyPath);

		if (is == null) {
			throw new RuntimeException("Public key not found: " + publicKeyPath);
		}

		String key = new String(is.readAllBytes()).replace("-----BEGIN PUBLIC KEY-----", "")
				.replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");

		byte[] decoded = Base64.getDecoder().decode(key);

		publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
	}

	public RSAPublicKey getPublicKey() {
		return publicKey;
	}
}