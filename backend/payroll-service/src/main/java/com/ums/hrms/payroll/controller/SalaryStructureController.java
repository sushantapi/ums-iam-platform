package com.ums.hrms.payroll.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ums.hrms.payroll.dto.CreateSalaryStructureRequest;
import com.ums.hrms.payroll.dto.SalaryStructureResponse;
import com.ums.hrms.payroll.dto.SupersedeSalaryStructureRequest;
import com.ums.hrms.payroll.service.SalaryStructureService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/hrms/payroll/salary-structures")
@RequiredArgsConstructor
public class SalaryStructureController {

    private final SalaryStructureService salaryStructureService;

    @PostMapping
    @PreAuthorize("hasAuthority('PAYROLL_STRUCTURE_MANAGE')")
    public ResponseEntity<SalaryStructureResponse> create(
            @Valid @RequestBody CreateSalaryStructureRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(salaryStructureService.create(
                        request,
                        currentUserId(authentication),
                        isSuperAdmin(authentication)));
    }

    @PostMapping("/{id}/supersede")
    @PreAuthorize("hasAuthority('PAYROLL_STRUCTURE_MANAGE')")
    public ResponseEntity<SalaryStructureResponse> supersede(
            @PathVariable UUID id,
            @Valid @RequestBody SupersedeSalaryStructureRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(salaryStructureService.supersede(
                        id,
                        request,
                        currentUserId(authentication),
                        isSuperAdmin(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    public List<SalaryStructureResponse> list(
            @RequestParam UUID organizationId,
            @RequestParam UUID employeeId,
            Authentication authentication) {
        return salaryStructureService.list(
                organizationId,
                employeeId,
                currentUserId(authentication),
                isSuperAdmin(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    public SalaryStructureResponse get(
            @PathVariable UUID id,
            @RequestParam UUID organizationId,
            Authentication authentication) {
        return salaryStructureService.get(
                id,
                organizationId,
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
