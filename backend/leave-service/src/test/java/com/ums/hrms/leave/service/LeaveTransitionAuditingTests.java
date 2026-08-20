package com.ums.hrms.leave.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ums.hrms.leave.config.JpaAuditConfig;
import com.ums.hrms.leave.dto.LeaveResponse;
import com.ums.hrms.leave.dto.LeaveTransitionRequest;
import com.ums.hrms.leave.entity.LeaveRequest;
import com.ums.hrms.leave.entity.LeaveStatus;
import com.ums.hrms.leave.entity.LeaveType;
import com.ums.hrms.leave.repository.LeaveRequestRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "management.health.rabbit.enabled=false"
})
@Import(JpaAuditConfig.class)
class LeaveTransitionAuditingTests {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    private LeaveService leaveService;

    private final UUID organizationId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID actorUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        leaveService = new LeaveService(
                leaveRequestRepository,
                mock(OrganizationAccessService.class),
                mock(LeaveTenantValidationService.class),
                mock(LeaveAuditPublisher.class));
    }

    @ParameterizedTest(name = "{0} transition advances updatedAt")
    @EnumSource(value = LeaveStatus.class, names = {"APPROVED", "REJECTED", "CANCELLED"})
    void terminalTransitionAdvancesUpdatedAtInResponseAndPersistence(LeaveStatus targetStatus)
            throws InterruptedException {
        LeaveRequest pending = new LeaveRequest();
        pending.setOrganizationId(organizationId);
        pending.setEmployeeId(employeeId);
        pending.setLeaveType(LeaveType.ANNUAL);
        pending.setStartDate(LocalDate.of(2026, 10, 1));
        pending.setEndDate(LocalDate.of(2026, 10, 2));
        pending.setStatus(LeaveStatus.PENDING);
        pending.setRequestedBy(actorUserId);

        leaveRequestRepository.saveAndFlush(pending);
        UUID leaveId = pending.getId();

        entityManager.clear();
        LeaveRequest beforeTransition = leaveRequestRepository
                .findByIdAndOrganizationId(leaveId, organizationId)
                .orElseThrow();
        LocalDateTime originalCreatedAt = beforeTransition.getCreatedAt();
        LocalDateTime originalUpdatedAt = beforeTransition.getUpdatedAt();
        assertNotNull(originalCreatedAt);
        assertNotNull(originalUpdatedAt);

        entityManager.clear();
        TimeUnit.MILLISECONDS.sleep(5);

        LeaveTransitionRequest request = new LeaveTransitionRequest(
                organizationId,
                "Auditing transition test");

        LeaveResponse response = switch (targetStatus) {
            case APPROVED -> leaveService.approve(leaveId, request, actorUserId, false);
            case REJECTED -> leaveService.reject(leaveId, request, actorUserId, false);
            case CANCELLED -> leaveService.cancel(leaveId, request, actorUserId, false);
            default -> throw new IllegalArgumentException("Unsupported terminal status: " + targetStatus);
        };

        assertEquals(targetStatus, response.status());
        assertEquals(originalCreatedAt, response.createdAt());
        assertNotNull(response.updatedAt());
        assertTrue(
                response.updatedAt().isAfter(originalUpdatedAt),
                () -> "Response updatedAt must advance for " + targetStatus
                        + ": before=" + originalUpdatedAt
                        + ", after=" + response.updatedAt());

        entityManager.clear();
        LeaveRequest persisted = leaveRequestRepository
                .findByIdAndOrganizationId(leaveId, organizationId)
                .orElseThrow();

        assertEquals(targetStatus, persisted.getStatus());
        assertEquals(originalCreatedAt, persisted.getCreatedAt());
        assertNotNull(persisted.getUpdatedAt());
        assertTrue(
                persisted.getUpdatedAt().isAfter(originalUpdatedAt),
                () -> "Persisted updatedAt must advance for " + targetStatus
                        + ": before=" + originalUpdatedAt
                        + ", after=" + persisted.getUpdatedAt());
    }
}
