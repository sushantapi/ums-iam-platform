package com.ums.hrms.attendance.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.ums.hrms.attendance.entity.AttendanceStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAttendanceRequest(
        @NotNull UUID organizationId,
        @NotNull UUID employeeId,
        @NotNull LocalDate workDate,
        @NotNull AttendanceStatus status,
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt,
        @Size(max = 500) String notes) {
}
