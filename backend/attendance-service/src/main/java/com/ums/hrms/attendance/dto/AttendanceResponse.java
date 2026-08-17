package com.ums.hrms.attendance.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.ums.hrms.attendance.entity.AttendanceStatus;

public record AttendanceResponse(
        UUID id,
        UUID organizationId,
        UUID employeeId,
        LocalDate workDate,
        AttendanceStatus status,
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt,
        String notes,
        UUID createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
