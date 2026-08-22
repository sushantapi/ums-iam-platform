package com.ums.auth.security.mfa;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

@Service
public class MfaRecoveryCodeService {

	private static final Base32 BASE32 = new Base32();

	private final MfaProperties properties;
	private final SecureRandom secureRandom;

	public MfaRecoveryCodeService(MfaProperties properties) {
		this(properties, new SecureRandom());
	}

	MfaRecoveryCodeService(MfaProperties properties, SecureRandom secureRandom) {
		this.properties = properties;
		this.secureRandom = secureRandom;
		if (properties.getRecoveryCodeCount() < 1 || properties.getRecoveryCodeCount() > 20) {
			throw new IllegalStateException("security.mfa.recovery-code-count must be between 1 and 20");
		}
		if (properties.getRecoveryCodeBytes() < 10) {
			throw new IllegalStateException("security.mfa.recovery-code-bytes must be at least 10");
		}
	}

	public List<String> generateCodes() {
		Set<String> codes = new LinkedHashSet<>();
		while (codes.size() < properties.getRecoveryCodeCount()) {
			byte[] entropy = new byte[properties.getRecoveryCodeBytes()];
			secureRandom.nextBytes(entropy);
			String compact = BASE32.encodeToString(entropy).replace("=", "").toUpperCase(Locale.ROOT);
			codes.add(group(compact));
		}
		return List.copyOf(codes);
	}

	public String hash(String rawCode) {
		String normalized = normalize(rawCode);
		if (normalized.isBlank()) {
			throw new IllegalArgumentException("MFA recovery code is required");
		}
		return DigestUtils.sha256Hex(normalized);
	}

	public boolean matches(String rawCode, String storedHash) {
		if (rawCode == null || storedHash == null || storedHash.isBlank()) {
			return false;
		}
		String candidateHash;
		try {
			candidateHash = hash(rawCode);
		} catch (IllegalArgumentException ex) {
			return false;
		}
		return MessageDigest.isEqual(
				candidateHash.getBytes(StandardCharsets.US_ASCII),
				storedHash.getBytes(StandardCharsets.US_ASCII));
	}

	private String normalize(String rawCode) {
		if (rawCode == null) {
			return "";
		}
		return rawCode.replace("-", "").replaceAll("\\s", "").toUpperCase(Locale.ROOT);
	}

	private String group(String compact) {
		List<String> groups = new ArrayList<>();
		for (int index = 0; index < compact.length(); index += 4) {
			groups.add(compact.substring(index, Math.min(index + 4, compact.length())));
		}
		return String.join("-", groups);
	}
}
