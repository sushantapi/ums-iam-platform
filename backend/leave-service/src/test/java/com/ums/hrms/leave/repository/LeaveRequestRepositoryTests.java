package com.ums.hrms.leave.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.ums.hrms.leave.config.JpaAuditConfig;
import com.ums.hrms.leave.entity.LeaveRequest;
import com.ums.hrms.leave.entity.LeaveStatus;
import com.ums.hrms.leave.entity.LeaveType;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(JpaAuditConfig.class)
class LeaveRequestRepositoryTests {

    private final LeaveRequestRepository leaveRequestRepository;

    private final UUID organizationId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID actorUserId = UUID.randomUUID();

    LeaveRequestRepositoryTests(LeaveRequestRepository leaveRequestRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
    }

    @Test
    void overlappingPendingRequestBlocksNewRequest() {
        persist(LeaveStatus.PENDING, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3));

        assertTrue(overlaps(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 4)));
    }

    @Test
    void overlappingApprovedRequestBlocksNewRequest() {
        persist(LeaveStatus.APPROVED, LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 7));

        assertTrue(overlaps(LocalDate.of(2026, 9, 6), LocalDate.of(2026, 9, 8)));
    }

    @Test
    void rejectedRequestDoesNotBlockNewRequest() {
        persist(LeaveStatus.REJECTED, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12));

        assertFalse(overlaps(LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 13)));
    }

    @Test
    void cancelledRequestDoesNotBlockNewRequest() {
        persist(LeaveStatus.CANCELLED, LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 17));

        assertFalse(overlaps(LocalDate.of(2026, 9, 16), LocalDate.of(2026, 9, 18)));
    }

    @Test
    void sharedBoundaryDateCountsAsOverlap() {
        persist(LeaveStatus.PENDING, LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 22));

        assertTrue(overlaps(LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 24)));
    }

    private boolean overlaps(LocalDate startDate, LocalDate endDate) {
        return leaveRequestRepository.existsOverlappingActiveRequest(
                organizationId,
                employeeId,
                startDate,
                endDate);
    }

    private void persist(LeaveStatus status, LocalDate startDate, LocalDate endDate) {
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setOrganizationId(organizationId);
        leaveRequest.setEmployeeId(employeeId);
        leaveRequest.setLeaveType(LeaveType.ANNUAL);
        leaveRequest.setStartDate(startDate);
        leaveRequest.setEndDate(endDate);
        leaveRequest.setStatus(status);
        leaveRequest.setRequestedBy(actorUserId);
        leaveRequestRepository.saveAndFlush(leaveRequest);
    }
}
