package com.ums.auth.security.mfa;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TotpService {

	private static final String HMAC_ALGORITHM = "HmacSHA1";
	private static final Base32 BASE32 = new Base32();

	private final MfaProperties properties;
	private final SecureRandom secureRandom;

	@Autowired
	public TotpService(MfaProperties properties) {
		this(properties, new SecureRandom());
	}

	TotpService(MfaProperties properties, SecureRandom secureRandom) {
		this.properties = properties;
		this.secureRandom = secureRandom;
		validateConfiguration();
	}

	public String generateSecret() {
		byte[] secret = new byte[properties.getTotp().getSecretBytes()];
		secureRandom.nextBytes(secret);
		return BASE32.encodeToString(secret).replace("=", "").toUpperCase(Locale.ROOT);
	}

	public boolean verify(String base32Secret, String suppliedCode, Instant now) {
		if (base32Secret == null || base32Secret.isBlank() || suppliedCode == null) {
			return false;
		}

		String code = suppliedCode.trim();
		int digits = properties.getTotp().getDigits();
		if (!code.matches("\\d{" + digits + "}")) {
			return false;
		}

		long currentCounter = Math.floorDiv(now.getEpochSecond(), properties.getTotp().getPeriodSeconds());
		for (int offset = -properties.getTotp().getAllowedWindow();
				offset <= properties.getTotp().getAllowedWindow();
				offset++) {
			long counter = currentCounter + offset;
			if (counter < 0) {
				continue;
			}
			String expected = generateCodeForCounter(base32Secret, counter, digits);
			if (MessageDigest.isEqual(
					expected.getBytes(StandardCharsets.US_ASCII),
					code.getBytes(StandardCharsets.US_ASCII))) {
				return true;
			}
		}
		return false;
	}

	public String generateCode(String base32Secret, Instant instant) {
		long counter = Math.floorDiv(instant.getEpochSecond(), properties.getTotp().getPeriodSeconds());
		return generateCodeForCounter(base32Secret, counter, properties.getTotp().getDigits());
	}

	public String provisioningUri(String accountName, String base32Secret) {
		if (accountName == null || accountName.isBlank()) {
			throw new IllegalArgumentException("MFA account name is required");
		}
		if (base32Secret == null || base32Secret.isBlank()) {
			throw new IllegalArgumentException("MFA secret is required");
		}

		String issuer = properties.getTotp().getIssuer();
		String label = encode(issuer + ":" + accountName.trim().toLowerCase(Locale.ROOT));
		return "otpauth://totp/" + label
				+ "?secret=" + encode(base32Secret)
				+ "&issuer=" + encode(issuer)
				+ "&algorithm=SHA1"
				+ "&digits=" + properties.getTotp().getDigits()
				+ "&period=" + properties.getTotp().getPeriodSeconds();
	}

	private String generateCodeForCounter(String base32Secret, long counter, int digits) {
		try {
			byte[] secret = BASE32.decode(base32Secret.replace(" ", "").toUpperCase(Locale.ROOT));
			if (secret.length == 0) {
				throw new IllegalArgumentException("MFA secret is invalid");
			}

			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
			byte[] hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(counter).array());
			int offset = hash[hash.length - 1] & 0x0f;
			int binary = ((hash[offset] & 0x7f) << 24)
					| ((hash[offset + 1] & 0xff) << 16)
					| ((hash[offset + 2] & 0xff) << 8)
					| (hash[offset + 3] & 0xff);

			int modulus = 1;
			for (int index = 0; index < digits; index++) {
				modulus *= 10;
			}
			int otp = binary % modulus;
			return String.format(Locale.ROOT, "%0" + digits + "d", otp);
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Unable to generate MFA TOTP code", ex);
		}
	}

	private void validateConfiguration() {
		MfaProperties.Totp totp = properties.getTotp();
		if (totp.getDigits() < 6 || totp.getDigits() > 8) {
			throw new IllegalStateException("security.mfa.totp.digits must be between 6 and 8");
		}
		if (totp.getPeriodSeconds() <= 0) {
			throw new IllegalStateException("security.mfa.totp.period-seconds must be positive");
		}
		if (totp.getAllowedWindow() < 0 || totp.getAllowedWindow() > 2) {
			throw new IllegalStateException("security.mfa.totp.allowed-window must be between 0 and 2");
		}
		if (totp.getSecretBytes() < 20) {
			throw new IllegalStateException("security.mfa.totp.secret-bytes must be at least 20");
		}
		if (totp.getIssuer() == null || totp.getIssuer().isBlank()) {
			throw new IllegalStateException("security.mfa.totp.issuer must not be blank");
		}
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}
}
