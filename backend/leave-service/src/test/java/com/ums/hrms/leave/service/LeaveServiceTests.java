package com.ums.hrms.leave.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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

import com.ums.hrms.leave.dto.CreateLeaveRequest;
import com.ums.hrms.leave.dto.LeaveTransitionRequest;
import com.ums.hrms.leave.entity.LeaveRequest;
import com.ums.hrms.leave.entity.LeaveStatus;
import com.ums.hrms.leave.entity.LeaveType;
import com.ums.hrms.leave.repository.LeaveRequestRepository;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTests {

    @Mock LeaveRequestRepository leaveRequestRepository;
    @Mock OrganizationAccessService organizationAccessService;
    @Mock LeaveTenantValidationService employeeValidationService;
    @InjectMocks LeaveService leaveService;

    private final UUID organizationId = UUID.randomUUID();
    private final UUID otherOrganizationId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID actorUserId = UUID.randomUUID();

    @Test
    void createPersistsPendingRequestForAuthenticatedActor() {
        CreateLeaveRequest request = request(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3), "  Vacation  ");
        when(leaveRequestRepository.existsOverlappingActiveRequest(
                organizationId, employeeId, request.startDate(), request.endDate())).thenReturn(false);
        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = leaveService.create(request, actorUserId, false);

        verify(organizationAccessService).assertCanAccess(organizationId, actorUserId, false);
        verify(employeeValidationService).validateEmployeeBelongsToOrganization(employeeId, organizationId);
        assertEquals(LeaveStatus.PENDING, response.status());
        assertEquals(actorUserId, response.requestedBy());
        assertEquals("Vacation", response.reason());
    }

    @Test
    void createRejectsReversedDates() {
        CreateLeaveRequest request = request(LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 3), null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> leaveService.create(request, actorUserId, false));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(leaveRequestRepository, never()).existsOverlappingActiveRequest(any(), any(), any(), any());
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void createRejectsOverlappingPendingRequest() {
        CreateLeaveRequest request = request(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3), null);
        when(leaveRequestRepository.existsOverlappingActiveRequest(
                organizationId, employeeId, request.startDate(), request.endDate())).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> leaveService.create(request, actorUserId, false));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void createRejectsOverlappingApprovedRequest() {
        CreateLeaveRequest request = request(LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 7), null);
        when(leaveRequestRepository.existsOverlappingActiveRequest(
                organizationId, employeeId, request.startDate(), request.endDate())).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> leaveService.create(request, actorUserId, false));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void rejectedAndCancelledHistoricalRequestsDoNotBlockNewRequest() {
        CreateLeaveRequest request = request(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), null);
        when(leaveRequestRepository.existsOverlappingActiveRequest(
                organizationId, employeeId, request.startDate(), request.endDate())).thenReturn(false);
        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = leaveService.create(request, actorUserId, false);

        assertEquals(LeaveStatus.PENDING, response.status());
        verify(leaveRequestRepository).existsOverlappingActiveRequest(
                organizationId, employeeId, request.startDate(), request.endDate());
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    void listEnforcesOrganizationAccessAndTenantScope() {
        LeaveRequest leave = leave(organizationId, employeeId, LeaveStatus.PENDING);
        when(leaveRequestRepository.findAllByOrganizationId(any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(leave), PageRequest.of(0, 20), 1));

        var response = leaveService.list(organizationId, 0, 20, actorUserId, false);

        verify(organizationAccessService).assertCanAccess(organizationId, actorUserId, false);
        verify(leaveRequestRepository).findAllByOrganizationId(eq(organizationId), any());
        assertEquals(organizationId, response.content().getFirst().organizationId());
    }

    @Test
    void getUsesTenantScopedLookupAndCannotReturnOtherOrganizationData() {
        UUID leaveId = UUID.randomUUID();
        when(leaveRequestRepository.findByIdAndOrganizationId(leaveId, otherOrganizationId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> leaveService.get(leaveId, otherOrganizationId, actorUserId, false));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(organizationAccessService).assertCanAccess(otherOrganizationId, actorUserId, false);
        verify(leaveRequestRepository).findByIdAndOrganizationId(leaveId, otherOrganizationId);
    }

    @Test
    void listRejectsInvalidPageAndSize() {
        ResponseStatusException negativePage = assertThrows(
                ResponseStatusException.class,
                () -> leaveService.list(organizationId, -1, 20, actorUserId, false));
        assertEquals(HttpStatus.BAD_REQUEST, negativePage.getStatusCode());

        ResponseStatusException zeroSize = assertThrows(
                ResponseStatusException.class,
                () -> leaveService.list(organizationId, 0, 0, actorUserId, false));
        assertEquals(HttpStatus.BAD_REQUEST, zeroSize.getStatusCode());

        ResponseStatusException oversized = assertThrows(
                ResponseStatusException.class,
                () -> leaveService.list(organizationId, 0, 201, actorUserId, false));
        assertEquals(HttpStatus.BAD_REQUEST, oversized.getStatusCode());

        verify(leaveRequestRepository, never()).findAllByOrganizationId(any(), any());
    }

    @Test
    void approveTransitionsPendingRequestAtomically() {
        UUID leaveId = UUID.randomUUID();
        LeaveRequest leave = leave(organizationId, employeeId, LeaveStatus.PENDING);
        LeaveTransitionRequest request = transitionRequest("  Approved by manager  ");
        when(leaveRequestRepository.findByIdAndOrganizationIdForUpdate(leaveId, organizationId))
                .thenReturn(Optional.of(leave));
        when(leaveRequestRepository.save(leave)).thenReturn(leave);

        var response = leaveService.approve(leaveId, request, actorUserId, false);

        verify(organizationAccessService).assertCanAccess(organizationId, actorUserId, false);
        verify(leaveRequestRepository).findByIdAndOrganizationIdForUpdate(leaveId, organizationId);
        assertEquals(LeaveStatus.APPROVED, response.status());
        assertEquals(actorUserId, response.decidedBy());
        assertNotNull(response.decidedAt());
        assertEquals("Approved by manager", response.decisionComment());
    }

    @Test
    void rejectTransitionsPendingRequest() {
        UUID leaveId = UUID.randomUUID();
        LeaveRequest leave = leave(organizationId, employeeId, LeaveStatus.PENDING);
        when(leaveRequestRepository.findByIdAndOrganizationIdForUpdate(leaveId, organizationId))
                .thenReturn(Optional.of(leave));
        when(leaveRequestRepository.save(leave)).thenReturn(leave);

        var response = leaveService.reject(leaveId, transitionRequest("Insufficient coverage"), actorUserId, false);

        assertEquals(LeaveStatus.REJECTED, response.status());
        assertEquals(actorUserId, response.decidedBy());
        assertEquals("Insufficient coverage", response.decisionComment());
    }

    @Test
    void cancelTransitionsPendingRequest() {
        UUID leaveId = UUID.randomUUID();
        LeaveRequest leave = leave(organizationId, employeeId, LeaveStatus.PENDING);
        when(leaveRequestRepository.findByIdAndOrganizationIdForUpdate(leaveId, organizationId))
                .thenReturn(Optional.of(leave));
        when(leaveRequestRepository.save(leave)).thenReturn(leave);

        var response = leaveService.cancel(leaveId, transitionRequest(null), actorUserId, false);

        assertEquals(LeaveStatus.CANCELLED, response.status());
        assertEquals(actorUserId, response.decidedBy());
        assertNotNull(response.decidedAt());
    }

    @Test
    void transitionRejectsNonPendingRequest() {
        UUID leaveId = UUID.randomUUID();
        LeaveRequest leave = leave(organizationId, employeeId, LeaveStatus.APPROVED);
        when(leaveRequestRepository.findByIdAndOrganizationIdForUpdate(leaveId, organizationId))
                .thenReturn(Optional.of(leave));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> leaveService.reject(leaveId, transitionRequest("Cannot change"), actorUserId, false));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void transitionUsesTenantScopedLockedLookup() {
        UUID leaveId = UUID.randomUUID();
        when(leaveRequestRepository.findByIdAndOrganizationIdForUpdate(leaveId, otherOrganizationId))
                .thenReturn(Optional.empty());
        LeaveTransitionRequest request = new LeaveTransitionRequest(otherOrganizationId, null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> leaveService.approve(leaveId, request, actorUserId, false));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(organizationAccessService).assertCanAccess(otherOrganizationId, actorUserId, false);
        verify(leaveRequestRepository).findByIdAndOrganizationIdForUpdate(leaveId, otherOrganizationId);
        verify(leaveRequestRepository, never()).save(any());
    }

    private CreateLeaveRequest request(LocalDate startDate, LocalDate endDate, String reason) {
        return new CreateLeaveRequest(
                organizationId,
                employeeId,
                LeaveType.ANNUAL,
                startDate,
                endDate,
                reason);
    }

    private LeaveTransitionRequest transitionRequest(String comment) {
        return new LeaveTransitionRequest(organizationId, comment);
    }

    private LeaveRequest leave(UUID organization, UUID employee, LeaveStatus status) {
        LeaveRequest leave = new LeaveRequest();
        leave.setOrganizationId(organization);
        leave.setEmployeeId(employee);
        leave.setLeaveType(LeaveType.ANNUAL);
        leave.setStartDate(LocalDate.of(2026, 9, 1));
        leave.setEndDate(LocalDate.of(2026, 9, 3));
        leave.setStatus(status);
        leave.setRequestedBy(actorUserId);
        return leave;
    }
}
