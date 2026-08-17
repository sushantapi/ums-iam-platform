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

import com.ums.hrms.employee.dto.CreateDesignationRequest;
import com.ums.hrms.employee.dto.DesignationPageResponse;
import com.ums.hrms.employee.dto.DesignationResponse;
import com.ums.hrms.employee.dto.UpdateDesignationRequest;
import com.ums.hrms.employee.service.DesignationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/hrms/designations")
@RequiredArgsConstructor
public class DesignationController {

    private final DesignationService designationService;

    @PostMapping
    @PreAuthorize("hasAuthority('DESIGNATION_CREATE')")
    public ResponseEntity<DesignationResponse> create(
            @Valid @RequestBody CreateDesignationRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(designationService.create(request, currentUserId(authentication), isSuperAdmin(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DESIGNATION_READ')")
    public DesignationPageResponse list(
            @RequestParam UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return designationService.list(
                organizationId,
                page,
                size,
                currentUserId(authentication),
                isSuperAdmin(authentication));
    }

    @GetMapping("/{designationId}")
    @PreAuthorize("hasAuthority('DESIGNATION_READ')")
    public DesignationResponse get(
            @PathVariable UUID designationId,
            @RequestParam UUID organizationId,
            Authentication authentication) {
        return designationService.get(
                designationId,
                organizationId,
                currentUserId(authentication),
                isSuperAdmin(authentication));
    }

    @PutMapping("/{designationId}")
    @PreAuthorize("hasAuthority('DESIGNATION_UPDATE')")
    public DesignationResponse update(
            @PathVariable UUID designationId,
            @Valid @RequestBody UpdateDesignationRequest request,
            Authentication authentication) {
        return designationService.update(
                designationId,
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
