package com.ums.hrms.employee.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description) {
}
