package com.ums.hrms.leave.controller;

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

import com.ums.hrms.leave.dto.CreateLeaveRequest;
import com.ums.hrms.leave.dto.LeavePageResponse;
import com.ums.hrms.leave.dto.LeaveResponse;
import com.ums.hrms.leave.service.LeaveService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/hrms/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    @PreAuthorize("hasAuthority('LEAVE_REQUEST_CREATE')")
    public ResponseEntity<LeaveResponse> create(
            @Valid @RequestBody CreateLeaveRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leaveService.create(
                        request,
                        currentUserId(authentication),
                        isSuperAdmin(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LEAVE_READ')")
    public LeavePageResponse list(
            @RequestParam UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return leaveService.list(
                organizationId,
                page,
                size,
                currentUserId(authentication),
                isSuperAdmin(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LEAVE_READ')")
    public LeaveResponse get(
            @PathVariable UUID id,
            @RequestParam UUID organizationId,
            Authentication authentication) {
        return leaveService.get(
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
