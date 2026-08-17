package com.ums.hrms.employee.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ums.hrms.employee.entity.MasterDataStatus;

public record DepartmentResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        String description,
        MasterDataStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
