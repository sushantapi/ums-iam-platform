package com.ums.auth.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MfaRecoveryCodesResponse {
	private List<String> recoveryCodes;
}
