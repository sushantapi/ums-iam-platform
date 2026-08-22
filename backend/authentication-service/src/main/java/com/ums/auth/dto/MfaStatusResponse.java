package com.ums.auth.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MfaStatusResponse {
	private boolean enabled;
	private boolean setupPending;
	private Instant setupExpiresAt;
	private long recoveryCodesRemaining;
}
