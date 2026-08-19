package com.ums.hrms.leave.entity;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class LeaveRequestTest {
 @Test void defaultsIdStatusAndTimestamps(){LeaveRequest r=new LeaveRequest();r.setOrganizationId(UUID.randomUUID());r.setEmployeeId(UUID.randomUUID());r.setLeaveType(LeaveType.ANNUAL);r.setStartDate(LocalDate.of(2026,8,20));r.setEndDate(LocalDate.of(2026,8,22));r.setRequestedBy(UUID.randomUUID());r.onCreate();assertThat(r.getId()).isNotNull();assertThat(r.getStatus()).isEqualTo(LeaveStatus.PENDING);assertThat(r.getCreatedAt()).isNotNull();assertThat(r.getUpdatedAt()).isEqualTo(r.getCreatedAt());}
}
