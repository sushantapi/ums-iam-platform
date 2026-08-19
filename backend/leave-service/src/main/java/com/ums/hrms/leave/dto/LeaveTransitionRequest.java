package com.ums.hrms.leave.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeaveTransitionRequest(
        @NotNull UUID organizationId,
        @Size(max = 2000) String decisionComment) {
}
