package com.ums.gateway.config;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class RsaKeyConfig {

	@Bean
	JwtDecoder jwtDecoder(@Value("${jwt.public-key-path}") String publicKeyPath) throws Exception {

		var resource = new ClassPathResource(publicKeyPath);

		String key = new String(resource.getInputStream().readAllBytes()).replace("-----BEGIN PUBLIC KEY-----", "")
				.replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");

		byte[] decoded = Base64.getDecoder().decode(key);

		X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);

		RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);

		return NimbusJwtDecoder.withPublicKey(publicKey).build();
	}
}