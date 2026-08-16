package com.ums.gateway.config;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.util.StringUtils;

@Configuration
public class RsaKeyConfig {

	@Bean
	RSAPublicKey rsaPublicKey(
			@Value("${jwt.public-key-path:}") String publicKeyPath,
			@Value("${jwt.public-key:}") String publicKeyPem) throws Exception {

		String key = readPublicKeyMaterial(publicKeyPath, publicKeyPem).replace("-----BEGIN PUBLIC KEY-----", "")
				.replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");

		byte[] decoded = Base64.getDecoder().decode(key);

		X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);

		return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
	}

	@Bean
	JwtDecoder jwtDecoder(RSAPublicKey publicKey, OAuth2TokenValidator<Jwt> jwtValidator) {

		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
		decoder.setJwtValidator(jwtValidator);
		return decoder;
	}

	@Bean
	ReactiveJwtDecoder reactiveJwtDecoder(RSAPublicKey publicKey, OAuth2TokenValidator<Jwt> jwtValidator) {
		NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();
		decoder.setJwtValidator(jwtValidator);
		return decoder;
	}

	@Bean
	OAuth2TokenValidator<Jwt> jwtValidator(
			@Value("${jwt.issuer:ums-iam-platform}") String issuer,
			@Value("${jwt.audience:}") String audience) {
		OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
		OAuth2TokenValidator<Jwt> accessTokenValidator = accessTokenValidator();

		if (!StringUtils.hasText(audience)) {
			return new DelegatingOAuth2TokenValidator<>(issuerValidator, accessTokenValidator);
		}

		return new DelegatingOAuth2TokenValidator<>(
				issuerValidator,
				accessTokenValidator,
				audienceValidator(audience));
	}

	private OAuth2TokenValidator<Jwt> accessTokenValidator() {
		return jwt -> {
			if ("ACCESS".equals(jwt.getClaimAsString("type"))) {
				return OAuth2TokenValidatorResult.success();
			}

			OAuth2Error error = new OAuth2Error(
					"invalid_token",
					"Only ACCESS tokens may authenticate gateway routes",
					null);
			return OAuth2TokenValidatorResult.failure(error);
		};
	}

	private OAuth2TokenValidator<Jwt> audienceValidator(String audience) {
		return jwt -> {
			if (jwt.getAudience().contains(audience)) {
				return OAuth2TokenValidatorResult.success();
			}

			OAuth2Error error = new OAuth2Error(
					"invalid_token",
					"JWT audience does not contain required audience: " + audience,
					null);
			return OAuth2TokenValidatorResult.failure(error);
		};
	}

	private String readPublicKeyMaterial(String publicKeyPath, String publicKeyPem) throws Exception {

		if (StringUtils.hasText(publicKeyPem)) {
			return publicKeyPem.replace("\\n", "\n");
		}

		if (!StringUtils.hasText(publicKeyPath)) {
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
