package com.ums.admin.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ums.admin.dto.response.UserDetailResponse;
import com.ums.admin.dto.response.UserSummaryPageResponse;
import com.ums.admin.dto.response.UserRoleAssignmentResponse;
import com.ums.admin.dto.response.OrganizationAdminResponse;
import com.ums.admin.service.AdminOrganizationService;
import com.ums.admin.service.AdminRoleService;
import com.ums.admin.service.AdminUserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','USER_ADMIN','SUPPORT') or hasAuthority('USER_READ')")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminRoleService adminRoleService;
    private final AdminOrganizationService adminOrganizationService;

    @GetMapping
    public UserSummaryPageResponse getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String organizationId,
            @RequestParam(required = false) String role) {
        validatePage(page, size);
        validateFilter("search", search);
        validateFilter("status", status);
        validateFilter("organizationId", organizationId);
        validateFilter("role", role);
        if (hasText(status) || hasText(organizationId) || hasText(role)) {
            throw badRequest("status, organizationId, and role filters are not supported by the current user profile schema");
        }
        return adminUserService.getUsers(page, size, search);
    }

    @GetMapping("/{userId}")
    public UserDetailResponse getUserById(@PathVariable UUID userId) {
        return adminUserService.getUserById(userId);
    }

    @GetMapping("/{userId}/roles")
    public java.util.List<UserRoleAssignmentResponse> getUserRoles(@PathVariable UUID userId) {
        return adminRoleService.getUserRoles(userId);
    }

    @GetMapping("/{userId}/organizations")
    public java.util.List<OrganizationAdminResponse> getUserOrganizations(@PathVariable UUID userId) {
        return adminOrganizationService.getForUser(userId);
    }

    @PostMapping("/{userId}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','USER_ADMIN') or hasAuthority('USER_WRITE')")
    public ResponseEntity<Void> activateUser(@PathVariable UUID userId, Authentication authentication) {
        adminUserService.activateUser(userId, currentAdminId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/suspend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','USER_ADMIN') or hasAuthority('USER_WRITE')")
    public ResponseEntity<Void> suspendUser(@PathVariable UUID userId, Authentication authentication) {
        adminUserService.suspendUser(userId, currentAdminId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/unlock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','USER_ADMIN') or hasAuthority('USER_WRITE')")
    public ResponseEntity<Void> unlockUser(@PathVariable UUID userId, Authentication authentication) {
        adminUserService.unlockUser(userId, currentAdminId(authentication));
        return ResponseEntity.noContent().build();
    }

    private UUID currentAdminId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || page > 100_000) {
            throw badRequest("page must be between 0 and 100000");
        }
        if (size < 1 || size > 200) {
            throw badRequest("size must be between 1 and 200");
        }
    }

    private void validateFilter(String name, String value) {
        if (value != null && value.length() > 255) {
            throw badRequest(name + " must not exceed 255 characters");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private org.springframework.web.server.ResponseStatusException badRequest(String reason) {
        return new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, reason);
    }
}
