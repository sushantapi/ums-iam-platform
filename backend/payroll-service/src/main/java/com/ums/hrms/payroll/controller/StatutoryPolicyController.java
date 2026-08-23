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

import com.ums.hrms.payroll.dto.CreateStatutoryPolicyRequest;
import com.ums.hrms.payroll.dto.StatutoryPolicyResponse;
import com.ums.hrms.payroll.service.StatutoryPolicyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/hrms/payroll/statutory-policies")
@RequiredArgsConstructor
public class StatutoryPolicyController {

    private final StatutoryPolicyService statutoryPolicyService;

    @PostMapping
    @PreAuthorize("hasAuthority('PAYROLL_STRUCTURE_MANAGE')")
    public ResponseEntity<StatutoryPolicyResponse> create(
            @Valid @RequestBody CreateStatutoryPolicyRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(statutoryPolicyService.create(
                        request,
                        currentUserId(authentication),
                        isSuperAdmin(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    public List<StatutoryPolicyResponse> list(
            @RequestParam UUID organizationId,
            @RequestParam(defaultValue = "IN") String countryCode,
            Authentication authentication) {
        return statutoryPolicyService.list(
                organizationId,
                countryCode,
                currentUserId(authentication),
                isSuperAdmin(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    public StatutoryPolicyResponse get(
            @PathVariable UUID id,
            @RequestParam UUID organizationId,
            Authentication authentication) {
        return statutoryPolicyService.get(
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