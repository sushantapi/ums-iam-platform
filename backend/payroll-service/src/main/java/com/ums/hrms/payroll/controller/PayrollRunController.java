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

import com.ums.hrms.payroll.dto.CreatePayrollRunRequest;
import com.ums.hrms.payroll.dto.PayrollEntryResponse;
import com.ums.hrms.payroll.dto.PayrollRunResponse;
import com.ums.hrms.payroll.dto.PayrollTransitionRequest;
import com.ums.hrms.payroll.service.PayrollRunService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/hrms/payroll")
@RequiredArgsConstructor
public class PayrollRunController {

    private final PayrollRunService payrollRunService;

    @PostMapping("/runs")
    @PreAuthorize("hasAuthority('PAYROLL_RUN_MANAGE')")
    public ResponseEntity<PayrollRunResponse> create(
            @Valid @RequestBody CreatePayrollRunRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(payrollRunService.create(
                        request,
                        currentUserId(authentication),
                        isSuperAdmin(authentication)));
    }

    @GetMapping("/runs")
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    public List<PayrollRunResponse> list(
            @RequestParam UUID organizationId,
            Authentication authentication) {
        return payrollRunService.list(
                organizationId,
                currentUserId(authentication),
                isSuperAdmin(authentication));
    }

    @GetMapping("/runs/{id}")
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    public PayrollRunResponse get(
            @PathVariable UUID id,
            @RequestParam UUID organizationId,
            Authentication authentication) {
        return payrollRunService.get(
                id,
                organizationId,
                currentUserId(authentication),
                isSuperAdmin(authentication));
    }

    @PostMapping("/runs/{id}/process")
    @PreAuthorize("hasAuthority('PAYROLL_RUN_MANAGE')")
    public PayrollRunResponse process(
            @PathVariable UUID id,
            @Valid @RequestBody PayrollTransitionRequest request,
            Authentication authentication) {
        return payrollRunService.process(
                id,
                request,
                currentUserId(authentication),
                isSuperAdmin(authentication));
    }

    @PostMapping("/runs/{id}/finalize")
    @PreAuthorize("hasAuthority('PAYROLL_RUN_MANAGE')")
    public PayrollRunResponse finalizeRun(
            @PathVariable UUID id,
            @Valid @RequestBody PayrollTransitionRequest request,
            Authentication authentication) {
        return payrollRunService.finalizeRun(
                id,
                request,
                currentUserId(authentication),
                isSuperAdmin(authentication));
    }

    @GetMapping("/runs/{id}/entries")
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    public List<PayrollEntryResponse> listEntries(
            @PathVariable UUID id,
            @RequestParam UUID organizationId,
            Authentication authentication) {
        return payrollRunService.listEntries(
                id,
                organizationId,
                currentUserId(authentication),
                isSuperAdmin(authentication));
    }

    @GetMapping("/payslips/{entryId}")
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    public PayrollEntryResponse getPayslip(
            @PathVariable UUID entryId,
            @RequestParam UUID organizationId,
            Authentication authentication) {
        return payrollRunService.getPayslip(
                entryId,
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
