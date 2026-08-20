package com.ums.hrms.leave.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LeaveRequestTest {

    @Test
    void initializesIdentityAndPendingStatus() {
        LeaveRequest request = new LeaveRequest();
        request.setOrganizationId(UUID.randomUUID());
        request.setEmployeeId(UUID.randomUUID());
        request.setLeaveType(LeaveType.ANNUAL);
        request.setStartDate(LocalDate.of(2026, 8, 20));
        request.setEndDate(LocalDate.of(2026, 8, 22));
        request.setRequestedBy(UUID.randomUUID());

        assertThat(request.getId()).isNotNull();
        assertThat(request.getId().toString()).hasSize(36);
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(request.getCreatedAt()).isNull();
        assertThat(request.getUpdatedAt()).isNull();
    }
}
