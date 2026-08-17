package com.ums.hrms.attendance.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ums.hrms.attendance.entity.AttendanceStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAttendanceRequest(
        @NotNull UUID organizationId,
        @NotNull AttendanceStatus status,
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt,
        @Size(max = 500) String notes) {
}
