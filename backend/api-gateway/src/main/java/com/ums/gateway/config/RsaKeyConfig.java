package com.ums.gateway.config;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
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
	RSAPublicKey rsaPublicKey(@Value("${jwt.public-key-path}") String publicKeyPath) throws Exception {

		var resource = new ClassPathResource(publicKeyPath);

		String key = new String(resource.getInputStream().readAllBytes()).replace("-----BEGIN PUBLIC KEY-----", "")
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

		if (!StringUtils.hasText(audience)) {
			return issuerValidator;
		}

		return new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator(audience));
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
}
