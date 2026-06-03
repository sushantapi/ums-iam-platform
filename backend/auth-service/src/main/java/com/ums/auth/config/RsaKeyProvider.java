package com.ums.auth.config;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@Configuration
public class RsaKeyProvider {

	@Bean
	public JwtDecoder jwtDecoder() throws Exception {

		return NimbusJwtDecoder.withPublicKey(loadPublicKey()).build();
	}
	
	@Value("${rsa.private-key}")
	private Resource privateKeyResource;

	@Value("${rsa.public-key}")
	private Resource publicKeyResource;

	@Bean
	public JwtEncoder jwtEncoder() throws Exception {

		RSAKey rsaKey = new RSAKey.Builder(loadPublicKey()).privateKey(loadPrivateKey()).build();

		return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
	}

	private RSAPublicKey loadPublicKey() throws Exception {

	    try (InputStream inputStream = publicKeyResource.getInputStream()) {

	        String key = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

	        key = key.replace("-----BEGIN PUBLIC KEY-----", "")
	                 .replace("-----END PUBLIC KEY-----", "")
	                 .replaceAll("\\s+", "");

	        byte[] decoded = Base64.getDecoder().decode(key);

	        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);

	        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
	    }
	}
	private RSAPrivateKey loadPrivateKey() throws Exception {

	    try (InputStream inputStream = privateKeyResource.getInputStream()) {

	        String key = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

	        key = key.replace("-----BEGIN PRIVATE KEY-----", "")
	                 .replace("-----END PRIVATE KEY-----", "")
	                 .replaceAll("\\s+", "");

	        byte[] decoded = Base64.getDecoder().decode(key);

	        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);

	        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
	    }
	}
}