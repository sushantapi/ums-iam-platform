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

import com.ums.hrms.employee.dto.CreateEmployeeRequest;
import com.ums.hrms.employee.dto.EmployeePageResponse;
import com.ums.hrms.employee.dto.EmployeeResponse;
import com.ums.hrms.employee.dto.UpdateEmployeeRequest;
import com.ums.hrms.employee.service.EmployeeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/hrms/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_CREATE')")
    public ResponseEntity<EmployeeResponse> create(
            @Valid @RequestBody CreateEmployeeRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeService.create(request, currentUserId(authentication), isSuperAdmin(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    public EmployeePageResponse list(
            @RequestParam UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return employeeService.list(
                organizationId,
                page,
                size,
                currentUserId(authentication),
                isSuperAdmin(authentication));
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    public EmployeeResponse get(
            @PathVariable UUID employeeId,
            @RequestParam UUID organizationId,
            Authentication authentication) {
        return employeeService.get(
                employeeId,
                organizationId,
                currentUserId(authentication),
                isSuperAdmin(authentication));
    }

    @PutMapping("/{employeeId}")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    public EmployeeResponse update(
            @PathVariable UUID employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request,
            Authentication authentication) {
        return employeeService.update(
                employeeId,
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
