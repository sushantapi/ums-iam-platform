package com.ums.org.security;

import java.util.regex.Pattern;

public record IssuedInvitationToken(String rawToken, String tokenHash) {

	private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-f]{64}");

	public IssuedInvitationToken {
		if (rawToken == null || rawToken.isBlank()) {
			throw new IllegalArgumentException("rawToken is required");
		}
		if (tokenHash == null || !SHA256_HEX.matcher(tokenHash).matches()) {
			throw new IllegalArgumentException("tokenHash must be a lowercase SHA-256 hex digest");
		}
	}

	@Override
	public String toString() {
		return "IssuedInvitationToken[REDACTED]";
	}
}
