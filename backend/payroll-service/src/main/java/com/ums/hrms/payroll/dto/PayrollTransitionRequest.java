package com.ums.hrms.payroll.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record PayrollTransitionRequest(
        @NotNull UUID organizationId) {
}
