package com.ums.auth.security.mfa;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.mfa")
public class MfaProperties {

	private String encryptionKey = "";
	private int recoveryCodeCount = 10;
	private int recoveryCodeBytes = 10;
	private final Totp totp = new Totp();

	public String getEncryptionKey() {
		return encryptionKey;
	}

	public void setEncryptionKey(String encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	public int getRecoveryCodeCount() {
		return recoveryCodeCount;
	}

	public void setRecoveryCodeCount(int recoveryCodeCount) {
		this.recoveryCodeCount = recoveryCodeCount;
	}

	public int getRecoveryCodeBytes() {
		return recoveryCodeBytes;
	}

	public void setRecoveryCodeBytes(int recoveryCodeBytes) {
		this.recoveryCodeBytes = recoveryCodeBytes;
	}

	public Totp getTotp() {
		return totp;
	}

	public static class Totp {

		private int digits = 6;
		private int periodSeconds = 30;
		private int allowedWindow = 1;
		private int secretBytes = 20;
		private int setupExpiryMinutes = 10;
		private String issuer = "UMS IAM";

		public int getDigits() {
			return digits;
		}

		public void setDigits(int digits) {
			this.digits = digits;
		}

		public int getPeriodSeconds() {
			return periodSeconds;
		}

		public void setPeriodSeconds(int periodSeconds) {
			this.periodSeconds = periodSeconds;
		}

		public int getAllowedWindow() {
			return allowedWindow;
		}

		public void setAllowedWindow(int allowedWindow) {
			this.allowedWindow = allowedWindow;
		}

		public int getSecretBytes() {
			return secretBytes;
		}

		public void setSecretBytes(int secretBytes) {
			this.secretBytes = secretBytes;
		}

		public int getSetupExpiryMinutes() {
			return setupExpiryMinutes;
		}

		public void setSetupExpiryMinutes(int setupExpiryMinutes) {
			this.setupExpiryMinutes = setupExpiryMinutes;
		}

		public String getIssuer() {
			return issuer;
		}

		public void setIssuer(String issuer) {
			this.issuer = issuer;
		}
	}
}
