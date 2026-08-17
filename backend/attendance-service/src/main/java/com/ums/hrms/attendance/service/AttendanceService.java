package com.ums.hrms.attendance.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.attendance.dto.AttendancePageResponse;
import com.ums.hrms.attendance.dto.AttendanceResponse;
import com.ums.hrms.attendance.dto.CreateAttendanceRequest;
import com.ums.hrms.attendance.dto.UpdateAttendanceRequest;
import com.ums.hrms.attendance.entity.AttendanceRecord;
import com.ums.hrms.attendance.repository.AttendanceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final OrganizationAccessService organizationAccessService;
    private final EmployeeReferenceService employeeReferenceService;

    public AttendanceResponse create(CreateAttendanceRequest request, UUID actorUserId, boolean superAdmin) {
        organizationAccessService.assertCanAccess(request.organizationId(), actorUserId, superAdmin);
        employeeReferenceService.assertActiveEmployee(request.organizationId(), request.employeeId());
        validateTimes(request.checkInAt(), request.checkOutAt());

        if (attendanceRepository.existsByOrganizationIdAndEmployeeIdAndWorkDate(
                request.organizationId(), request.employeeId(), request.workDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance already exists for employee and date");
        }

        AttendanceRecord record = AttendanceRecord.builder()
                .organizationId(request.organizationId())
                .employeeId(request.employeeId())
                .workDate(request.workDate())
                .status(request.status())
                .checkInAt(request.checkInAt())
                .checkOutAt(request.checkOutAt())
                .notes(normalizeNotes(request.notes()))
                .createdBy(actorUserId)
                .build();

        return toResponse(attendanceRepository.save(record));
    }

    @Transactional(readOnly = true)
    public AttendancePageResponse list(
            UUID organizationId,
            int page,
            int size,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);
        validatePage(page, size);

        var records = attendanceRepository.findAllByOrganizationId(
                organizationId,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("workDate"), Sort.Order.desc("createdAt"))));

        return new AttendancePageResponse(
                records.getContent().stream().map(this::toResponse).toList(),
                records.getNumber(),
                records.getSize(),
                records.getTotalElements(),
                records.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AttendanceResponse get(UUID attendanceId, UUID organizationId, UUID actorUserId, boolean superAdmin) {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);
        return toResponse(findScoped(attendanceId, organizationId));
    }

    public AttendanceResponse update(
            UUID attendanceId,
            UpdateAttendanceRequest request,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(request.organizationId(), actorUserId, superAdmin);
        validateTimes(request.checkInAt(), request.checkOutAt());

        AttendanceRecord record = findScoped(attendanceId, request.organizationId());
        record.setStatus(request.status());
        record.setCheckInAt(request.checkInAt());
        record.setCheckOutAt(request.checkOutAt());
        record.setNotes(normalizeNotes(request.notes()));

        return toResponse(attendanceRepository.save(record));
    }

    private AttendanceRecord findScoped(UUID attendanceId, UUID organizationId) {
        return attendanceRepository.findByIdAndOrganizationId(attendanceId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance record not found"));
    }

    private void validateTimes(LocalDateTime checkInAt, LocalDateTime checkOutAt) {
        if (checkInAt != null && checkOutAt != null && checkOutAt.isBefore(checkInAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "checkOutAt cannot be before checkInAt");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or size");
        }
    }

    private String normalizeNotes(String notes) {
        if (notes == null) {
            return null;
        }
        String normalized = notes.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private AttendanceResponse toResponse(AttendanceRecord record) {
        return new AttendanceResponse(
                record.getId(),
                record.getOrganizationId(),
                record.getEmployeeId(),
                record.getWorkDate(),
                record.getStatus(),
                record.getCheckInAt(),
                record.getCheckOutAt(),
                record.getNotes(),
                record.getCreatedBy(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }
}
