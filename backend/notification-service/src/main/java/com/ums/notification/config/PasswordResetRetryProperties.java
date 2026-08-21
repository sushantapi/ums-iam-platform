package com.ums.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

@Component
@Validated
@ConfigurationProperties(prefix = "notification.password-reset.retry")
public class PasswordResetRetryProperties {

	@Min(1)
	@Max(10)
	private int maxAttempts = 3;

	@PositiveOrZero
	private long initialBackoffMs = 1000L;

	@DecimalMin("1.0")
	private double multiplier = 2.0d;

	@PositiveOrZero
	private long maxBackoffMs = 5000L;

	public long backoffAfterAttempt(int failedAttempt) {
		if (failedAttempt < 1 || failedAttempt >= maxAttempts
				|| initialBackoffMs == 0L || maxBackoffMs == 0L) {
			return 0L;
		}

		double calculated = initialBackoffMs * Math.pow(multiplier, failedAttempt - 1);
		if (!Double.isFinite(calculated) || calculated >= maxBackoffMs) {
			return maxBackoffMs;
		}

		return Math.min(maxBackoffMs, Math.max(0L, (long) calculated));
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}

	public void setMaxAttempts(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	public long getInitialBackoffMs() {
		return initialBackoffMs;
	}

	public void setInitialBackoffMs(long initialBackoffMs) {
		this.initialBackoffMs = initialBackoffMs;
	}

	public double getMultiplier() {
		return multiplier;
	}

	public void setMultiplier(double multiplier) {
		this.multiplier = multiplier;
	}

	public long getMaxBackoffMs() {
		return maxBackoffMs;
	}

	public void setMaxBackoffMs(long maxBackoffMs) {
		this.maxBackoffMs = maxBackoffMs;
	}
}
