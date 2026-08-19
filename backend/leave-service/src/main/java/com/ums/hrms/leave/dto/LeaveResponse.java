package com.ums.hrms.leave.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.ums.hrms.leave.entity.LeaveStatus;
import com.ums.hrms.leave.entity.LeaveType;

public record LeaveResponse(
        UUID id,
        UUID organizationId,
        UUID employeeId,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        LeaveStatus status,
        UUID requestedBy,
        UUID decidedBy,
        Instant decidedAt,
        String decisionComment,
        Instant createdAt,
        Instant updatedAt) {
}
