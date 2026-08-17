package com.ums.hrms.employee.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ums.hrms.employee.dto.CreateDepartmentRequest;
import com.ums.hrms.employee.dto.DepartmentPageResponse;
import com.ums.hrms.employee.dto.DepartmentResponse;
import com.ums.hrms.employee.dto.UpdateDepartmentRequest;
import com.ums.hrms.employee.service.DepartmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/hrms/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasAuthority('DEPARTMENT_CREATE')")
    public ResponseEntity<DepartmentResponse> create(
            @Valid @RequestBody CreateDepartmentRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(departmentService.create(request, currentUserId(authentication), isSuperAdmin(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DEPARTMENT_READ')")
    public DepartmentPageResponse list(
            @RequestParam UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return departmentService.list(
                organizationId,
                page,
                size,
                currentUserId(authentication),
                isSuperAdmin(authentication));
    }

    @GetMapping("/{departmentId}")
    @PreAuthorize("hasAuthority('DEPARTMENT_READ')")
    public DepartmentResponse get(
            @PathVariable UUID departmentId,
            @RequestParam UUID organizationId,
            Authentication authentication) {
        return departmentService.get(
                departmentId,
                organizationId,
                currentUserId(authentication),
                isSuperAdmin(authentication));
    }

    @PutMapping("/{departmentId}")
    @PreAuthorize("hasAuthority('DEPARTMENT_UPDATE')")
    public DepartmentResponse update(
            @PathVariable UUID departmentId,
            @Valid @RequestBody UpdateDepartmentRequest request,
            Authentication authentication) {
        return departmentService.update(
                departmentId,
                request,
                currentUserId(authentication),
                isSuperAdmin(authentication));
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString((String) authentication.getPrincipal());
    }

    private boolean isSuperAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }
}
