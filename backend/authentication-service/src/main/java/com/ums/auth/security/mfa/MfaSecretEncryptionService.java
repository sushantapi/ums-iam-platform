package com.ums.auth.security.mfa;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

@Service
public class MfaSecretEncryptionService {

	private static final String VERSION = "v1";
	private static final String CIPHER = "AES/GCM/NoPadding";
	private static final String KEY_ALGORITHM = "AES";
	private static final int IV_BYTES = 12;
	private static final int GCM_TAG_BITS = 128;
	private static final byte[] AAD = "ums-iam:mfa-secret:v1".getBytes(StandardCharsets.UTF_8);

	private final MfaProperties properties;
	private final SecureRandom secureRandom;

	public MfaSecretEncryptionService(MfaProperties properties) {
		this(properties, new SecureRandom());
	}

	MfaSecretEncryptionService(MfaProperties properties, SecureRandom secureRandom) {
		this.properties = properties;
		this.secureRandom = secureRandom;
	}

	public String encrypt(String plaintextSecret) {
		if (plaintextSecret == null || plaintextSecret.isBlank()) {
			throw new IllegalArgumentException("MFA secret is required");
		}

		try {
			byte[] iv = new byte[IV_BYTES];
			secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance(CIPHER);
			cipher.init(Cipher.ENCRYPT_MODE, resolveKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
			cipher.updateAAD(AAD);
			byte[] encrypted = cipher.doFinal(plaintextSecret.getBytes(StandardCharsets.UTF_8));
			return VERSION + "." + encode(iv) + "." + encode(encrypted);
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Unable to encrypt MFA secret", ex);
		}
	}

	public String decrypt(String encryptedSecret) {
		if (encryptedSecret == null || encryptedSecret.isBlank()) {
			throw new IllegalArgumentException("Encrypted MFA secret is required");
		}

		String[] parts = encryptedSecret.split("\\.", 3);
		if (parts.length != 3 || !VERSION.equals(parts[0])) {
			throw new IllegalArgumentException("Unsupported MFA secret envelope");
		}

		try {
			byte[] iv = decode(parts[1]);
			if (iv.length != IV_BYTES) {
				throw new IllegalArgumentException("Invalid MFA secret envelope");
			}
			byte[] ciphertext = decode(parts[2]);
			Cipher cipher = Cipher.getInstance(CIPHER);
			cipher.init(Cipher.DECRYPT_MODE, resolveKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
			cipher.updateAAD(AAD);
			return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException | IllegalArgumentException ex) {
			throw new IllegalStateException("Unable to decrypt MFA secret", ex);
		}
	}

	private SecretKey resolveKey() {
		String configured = properties.getEncryptionKey();
		if (configured == null || configured.isBlank()) {
			throw new IllegalStateException("MFA encryption key is not configured");
		}
		try {
			byte[] keyBytes = Base64.getDecoder().decode(configured.trim());
			if (keyBytes.length != 32) {
				throw new IllegalStateException("MFA encryption key must decode to 32 bytes");
			}
			return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
		} catch (IllegalArgumentException ex) {
			throw new IllegalStateException("MFA encryption key must be valid Base64", ex);
		}
	}

	private String encode(byte[] value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
	}

	private byte[] decode(String value) {
		return Base64.getUrlDecoder().decode(value);
	}
}
