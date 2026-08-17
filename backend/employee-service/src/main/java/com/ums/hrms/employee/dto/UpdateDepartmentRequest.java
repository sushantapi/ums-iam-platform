package com.ums.hrms.employee.dto;

import java.util.UUID;

import com.ums.hrms.employee.entity.MasterDataStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateDepartmentRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotNull MasterDataStatus status) {
}
