package com.ums.hrms.leave.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.ums.hrms.leave.entity.LeaveType;

import jakarta.validation.constraints.NotNull;

public record CreateLeaveRequest(
        @NotNull UUID organizationId,
        @NotNull UUID employeeId,
        @NotNull LeaveType leaveType,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String reason) {
}
