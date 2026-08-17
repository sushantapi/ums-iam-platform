package com.ums.hrms.attendance.controller;

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

import com.ums.hrms.attendance.dto.AttendancePageResponse;
import com.ums.hrms.attendance.dto.AttendanceResponse;
import com.ums.hrms.attendance.dto.CreateAttendanceRequest;
import com.ums.hrms.attendance.dto.UpdateAttendanceRequest;
import com.ums.hrms.attendance.service.AttendanceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/hrms/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_CREATE')")
    public ResponseEntity<AttendanceResponse> create(
            @Valid @RequestBody CreateAttendanceRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.create(request, currentUserId(authentication), isSuperAdmin(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_READ')")
    public AttendancePageResponse list(
            @RequestParam UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return attendanceService.list(
                organizationId,
                page,
                size,
                currentUserId(authentication),
                isSuperAdmin(authentication));
    }

    @GetMapping("/{attendanceId}")
    @PreAuthorize("hasAuthority('ATTENDANCE_READ')")
    public AttendanceResponse get(
            @PathVariable UUID attendanceId,
            @RequestParam UUID organizationId,
            Authentication authentication) {
        return attendanceService.get(
                attendanceId,
                organizationId,
                currentUserId(authentication),
                isSuperAdmin(authentication));
    }

    @PutMapping("/{attendanceId}")
    @PreAuthorize("hasAuthority('ATTENDANCE_UPDATE')")
    public AttendanceResponse update(
            @PathVariable UUID attendanceId,
            @Valid @RequestBody UpdateAttendanceRequest request,
            Authentication authentication) {
        return attendanceService.update(
                attendanceId,
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
