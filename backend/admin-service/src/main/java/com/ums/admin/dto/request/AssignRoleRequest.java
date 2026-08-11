package com.ums.admin.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignRoleRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotBlank(message = "roleName is required")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,63}$", message = "roleName must use uppercase role naming")
    private String roleName;

    @Pattern(regexp = "^(PLATFORM|ORG|DEPARTMENT)$", message = "scopeType must be PLATFORM, ORG, or DEPARTMENT")
    private String scopeType;

    @Size(max = 255, message = "scopeId must not exceed 255 characters")
    private String scopeId;

    private UUID assignedBy;
}
