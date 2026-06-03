package com.ums.authorization.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PermissionCheckResponse {

	private boolean allowed;
}