package com.ums.hrms.payroll.dto;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

import com.ums.hrms.payroll.entity.PayrollRunStatus;

public record PayrollRunResponse(
        UUID id,
        UUID organizationId,
        YearMonth payrollMonth,
        PayrollRunStatus status,
        UUID createdBy,
        UUID processedBy,
        LocalDateTime processedAt,
        UUID finalizedBy,
        LocalDateTime finalizedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
