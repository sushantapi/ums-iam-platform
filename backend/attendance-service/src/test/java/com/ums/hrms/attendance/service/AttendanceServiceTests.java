package com.ums.hrms.attendance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.attendance.dto.CreateAttendanceRequest;
import com.ums.hrms.attendance.dto.UpdateAttendanceRequest;
import com.ums.hrms.attendance.entity.AttendanceRecord;
import com.ums.hrms.attendance.entity.AttendanceStatus;
import com.ums.hrms.attendance.repository.AttendanceRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTests {

    @Mock AttendanceRepository attendanceRepository;
    @Mock OrganizationAccessService organizationAccessService;
    @Mock EmployeeReferenceService employeeReferenceService;
    @InjectMocks AttendanceService attendanceService;

    private final UUID organizationId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID actorUserId = UUID.randomUUID();

    @Test
    void createPersistsTenantScopedAttendance() {
        LocalDate workDate = LocalDate.of(2026, 8, 17);
        var request = new CreateAttendanceRequest(
                organizationId, employeeId, workDate, AttendanceStatus.PRESENT,
                LocalDateTime.of(2026, 8, 17, 9, 0),
                LocalDateTime.of(2026, 8, 17, 18, 0),
                " Office ");
        when(attendanceRepository.existsByOrganizationIdAndEmployeeIdAndWorkDate(
                organizationId, employeeId, workDate)).thenReturn(false);
        when(attendanceRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = attendanceService.create(request, actorUserId, false);

        verify(organizationAccessService).assertCanAccess(organizationId, actorUserId, false);
        verify(employeeReferenceService).assertActiveEmployee(organizationId, employeeId);
        assertEquals(employeeId, response.employeeId());
        assertEquals(workDate, response.workDate());
        assertEquals("Office", response.notes());
        assertEquals(actorUserId, response.createdBy());
    }

    @Test
    void createRejectsDuplicateDailyAttendance() {
        LocalDate workDate = LocalDate.of(2026, 8, 17);
        var request = new CreateAttendanceRequest(
                organizationId, employeeId, workDate, AttendanceStatus.PRESENT, null, null, null);
        when(attendanceRepository.existsByOrganizationIdAndEmployeeIdAndWorkDate(
                organizationId, employeeId, workDate)).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> attendanceService.create(request, actorUserId, false));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void createRejectsCheckoutBeforeCheckin() {
        var request = new CreateAttendanceRequest(
                organizationId,
                employeeId,
                LocalDate.of(2026, 8, 17),
                AttendanceStatus.PRESENT,
                LocalDateTime.of(2026, 8, 17, 18, 0),
                LocalDateTime.of(2026, 8, 17, 9, 0),
                null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> attendanceService.create(request, actorUserId, false));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void listReturnsOnlyRequestedOrganizationPage() {
        AttendanceRecord record = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .employeeId(employeeId)
                .workDate(LocalDate.of(2026, 8, 17))
                .status(AttendanceStatus.PRESENT)
                .createdBy(actorUserId)
                .build();
        when(attendanceRepository.findAllByOrganizationId(any(), any()))
                .thenReturn(new PageImpl<>(List.of(record), PageRequest.of(0, 20), 1));

        var response = attendanceService.list(organizationId, 0, 20, actorUserId, false);

        verify(organizationAccessService).assertCanAccess(organizationId, actorUserId, false);
        assertEquals(1, response.totalElements());
        assertEquals(employeeId, response.content().getFirst().employeeId());
    }

    @Test
    void updateKeepsEmployeeAndWorkDateImmutable() {
        UUID attendanceId = UUID.randomUUID();
        LocalDate workDate = LocalDate.of(2026, 8, 17);
        AttendanceRecord record = AttendanceRecord.builder()
                .id(attendanceId)
                .organizationId(organizationId)
                .employeeId(employeeId)
                .workDate(workDate)
                .status(AttendanceStatus.PRESENT)
                .createdBy(actorUserId)
                .build();
        when(attendanceRepository.findByIdAndOrganizationId(attendanceId, organizationId))
                .thenReturn(Optional.of(record));
        when(attendanceRepository.save(record)).thenReturn(record);

        var response = attendanceService.update(
                attendanceId,
                new UpdateAttendanceRequest(organizationId, AttendanceStatus.HALF_DAY, null, null, "Adjusted"),
                actorUserId,
                false);

        assertEquals(employeeId, response.employeeId());
        assertEquals(workDate, response.workDate());
        assertEquals(AttendanceStatus.HALF_DAY, response.status());
    }
}
