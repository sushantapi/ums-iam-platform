package com.ums.hrms.leave.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LeaveRequestTest {

    @Test
    void defaultsIdentityStatusAndTimestampsOnCreate() {
        LeaveRequest request = new LeaveRequest();
        request.setOrganizationId(UUID.randomUUID());
        request.setEmployeeId(UUID.randomUUID());
        request.setLeaveType(LeaveType.ANNUAL);
        request.setStartDate(LocalDate.of(2026, 8, 20));
        request.setEndDate(LocalDate.of(2026, 8, 22));
        request.setRequestedBy(UUID.randomUUID());

        request.onCreate();

        assertThat(request.getId()).isNotNull();
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(request.getCreatedAt()).isNotNull();
        assertThat(request.getUpdatedAt()).isEqualTo(request.getCreatedAt());
    }
}
