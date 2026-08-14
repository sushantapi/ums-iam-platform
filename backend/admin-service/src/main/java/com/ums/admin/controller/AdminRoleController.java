package com.ums.admin.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.admin.dto.request.AssignRoleRequest;
import com.ums.admin.dto.response.PermissionSummaryResponse;
import com.ums.admin.dto.response.RoleSummaryResponse;
import com.ums.admin.dto.response.UserRoleAssignmentResponse;
import com.ums.admin.service.AdminRoleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private static final java.util.Set<String> PRIVILEGED_ROLES = java.util.Set.of(
            "SUPER_ADMIN", "AUTH_ADMIN", "AUDIT_ADMIN", "SECURITY", "COMPLIANCE");

    private final AdminRoleService adminRoleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUTH_ADMIN','SUPPORT') or hasAuthority('ROLE_READ')")
    public List<RoleSummaryResponse> getRoles() {
        return adminRoleService.getRoles();
    }

    @GetMapping("/{roleId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUTH_ADMIN','SUPPORT') or hasAuthority('ROLE_READ')")
    public RoleSummaryResponse getRole(@PathVariable UUID roleId) {
        return adminRoleService.getRole(roleId);
    }

    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUTH_ADMIN','SUPPORT') or hasAuthority('ROLE_READ')")
    public List<PermissionSummaryResponse> getRolePermissions(@PathVariable UUID roleId) {
        return adminRoleService.getRolePermissions(roleId);
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUTH_ADMIN','USER_ADMIN','SUPPORT') or hasAuthority('ROLE_READ')")
    public List<UserRoleAssignmentResponse> getUserRoles(@PathVariable UUID userId) {
        return adminRoleService.getUserRoles(userId);
    }

    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> revokeRoleAssignment(@PathVariable UUID assignmentId) {
        adminRoleService.revokeRoleAssignment(assignmentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUTH_ADMIN') or hasAuthority('ROLE_WRITE')")
    public ResponseEntity<String> assignRole(
            @Valid @RequestBody AssignRoleRequest request,
            Authentication authentication
    ) {
        request.setAssignedBy((java.util.UUID) authentication.getPrincipal());
        if (PRIVILEGED_ROLES.contains(request.getRoleName())
                && authentication.getAuthorities().stream()
                        .noneMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()))) {
            throw new AccessDeniedException("Only SUPER_ADMIN may assign privileged platform roles");
        }
        if (request.getScopeType() == null || request.getScopeType().isBlank()) {
            request.setScopeType("PLATFORM");
        }
        if (request.getScopeId() == null || request.getScopeId().isBlank()) {
            request.setScopeId("*");
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(adminRoleService.assignRole(request));
    }
}
