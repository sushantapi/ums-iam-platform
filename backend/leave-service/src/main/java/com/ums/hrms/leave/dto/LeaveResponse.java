package com.ums.hrms.leave.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        LocalDateTime decidedAt,
        String decisionComment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
