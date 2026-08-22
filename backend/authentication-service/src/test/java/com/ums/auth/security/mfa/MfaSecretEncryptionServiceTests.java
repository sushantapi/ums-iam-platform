package com.ums.auth.security.mfa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class MfaSecretEncryptionServiceTests {

	@Test
	void encryptsAndDecryptsWithoutPersistingPlaintext() {
		MfaProperties properties = propertiesWithKey((byte) 7);
		MfaSecretEncryptionService service = new MfaSecretEncryptionService(properties);
		String plaintext = "JBSWY3DPEHPK3PXP";

		String encrypted = service.encrypt(plaintext);

		assertThat(encrypted).startsWith("v1.");
		assertThat(encrypted).doesNotContain(plaintext);
		assertThat(service.decrypt(encrypted)).isEqualTo(plaintext);
	}

	@Test
	void refusesCiphertextWhenEncryptionKeyIsWrong() {
		MfaSecretEncryptionService writer = new MfaSecretEncryptionService(propertiesWithKey((byte) 1));
		MfaSecretEncryptionService reader = new MfaSecretEncryptionService(propertiesWithKey((byte) 2));
		String encrypted = writer.encrypt("JBSWY3DPEHPK3PXP");

		assertThatThrownBy(() -> reader.decrypt(encrypted))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Unable to decrypt MFA secret");
	}

	@Test
	void requiresDedicatedAes256KeyMaterial() {
		MfaProperties properties = new MfaProperties();
		properties.setEncryptionKey(Base64.getEncoder().encodeToString(new byte[16]));
		MfaSecretEncryptionService service = new MfaSecretEncryptionService(properties);

		assertThatThrownBy(() -> service.encrypt("JBSWY3DPEHPK3PXP"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("MFA encryption key must decode to 32 bytes");
	}

	private MfaProperties propertiesWithKey(byte fill) {
		byte[] key = new byte[32];
		java.util.Arrays.fill(key, fill);
		MfaProperties properties = new MfaProperties();
		properties.setEncryptionKey(Base64.getEncoder().encodeToString(key));
		return properties;
	}
}
