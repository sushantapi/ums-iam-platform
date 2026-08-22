package com.ums.auth.security.mfa;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.Test;

class TotpServiceTests {

	@Test
	void matchesRfc6238Sha1Vectors() {
		MfaProperties properties = properties();
		properties.getTotp().setDigits(8);
		TotpService service = new TotpService(properties);
		String secret = new Base32().encodeToString("12345678901234567890".getBytes(StandardCharsets.US_ASCII))
				.replace("=", "");

		assertThat(service.generateCode(secret, Instant.ofEpochSecond(59))).isEqualTo("94287082");
		assertThat(service.generateCode(secret, Instant.ofEpochSecond(1_111_111_109L))).isEqualTo("07081804");
		assertThat(service.generateCode(secret, Instant.ofEpochSecond(1_111_111_111L))).isEqualTo("14050471");
		assertThat(service.generateCode(secret, Instant.ofEpochSecond(1_234_567_890L))).isEqualTo("89005924");
		assertThat(service.generateCode(secret, Instant.ofEpochSecond(2_000_000_000L))).isEqualTo("69279037");
		assertThat(service.generateCode(secret, Instant.ofEpochSecond(20_000_000_000L))).isEqualTo("65353130");
	}

	@Test
	void acceptsOnlyConfiguredClockWindow() {
		MfaProperties properties = properties();
		TotpService service = new TotpService(properties);
		String secret = service.generateSecret();
		Instant issuedAt = Instant.ofEpochSecond(1_800_000_000L);
		String code = service.generateCode(secret, issuedAt);

		assertThat(service.verify(secret, code, issuedAt)).isTrue();
		assertThat(service.verify(secret, code, issuedAt.plusSeconds(30))).isTrue();
		assertThat(service.verify(secret, code, issuedAt.plusSeconds(60))).isFalse();
	}

	@Test
	void buildsAuthenticatorCompatibleProvisioningUri() {
		MfaProperties properties = properties();
		TotpService service = new TotpService(properties);
		String secret = service.generateSecret();

		String uri = service.provisioningUri("User@example.com", secret);

		assertThat(uri).startsWith("otpauth://totp/UMS%20IAM%3Auser%40example.com?");
		assertThat(uri).contains("secret=" + secret);
		assertThat(uri).contains("issuer=UMS%20IAM");
		assertThat(uri).contains("algorithm=SHA1");
		assertThat(uri).contains("digits=6");
		assertThat(uri).contains("period=30");
	}

	private MfaProperties properties() {
		MfaProperties properties = new MfaProperties();
		properties.getTotp().setDigits(6);
		properties.getTotp().setPeriodSeconds(30);
		properties.getTotp().setAllowedWindow(1);
		properties.getTotp().setSecretBytes(20);
		properties.getTotp().setIssuer("UMS IAM");
		return properties;
	}
}
