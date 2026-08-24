package com.ums.hrms.payroll.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.hrms.payroll.dto.CreateSalaryStructureRequest;
import com.ums.hrms.payroll.entity.SalaryStructure;
import com.ums.hrms.payroll.repository.SalaryStructureRepository;

@ExtendWith(MockitoExtension.class)
class SalaryStructureVersionMetadataTests {

    @Mock SalaryStructureRepository repository;
    @Mock OrganizationAccessService organizationAccessService;
    @Mock PayrollTenantValidationService employeeValidationService;
    @Mock PayrollAuditPublisher payrollAuditPublisher;
    @InjectMocks SalaryStructureService service;

    @Test
    void firstSalaryStructureStartsAtVersionOne() {
        UUID organizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        CreateSalaryStructureRequest request = request(organizationId, employeeId, LocalDate.of(2026, 1, 1));

        when(repository.countOverlappingEffectiveRanges(
                organizationId, employeeId, request.effectiveFrom(), null)).thenReturn(0L);
        when(repository.findFirstByOrganizationIdAndEmployeeIdOrderByVersionNumberDesc(
                organizationId, employeeId)).thenReturn(Optional.empty());
        when(repository.save(any(SalaryStructure.class))).thenAnswer(invocation -> {
            SalaryStructure value = invocation.getArgument(0);
            value.setId(UUID.randomUUID());
            return value;
        });

        var response = service.create(request, actorUserId, false);

        assertEquals(1, response.versionNumber());
        assertNull(response.supersedesStructureId());
    }

    @Test
    void laterNonOverlappingCreateAppendsVersionLineage() {
        UUID organizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        SalaryStructure latest = new SalaryStructure();
        latest.setId(UUID.randomUUID());
        latest.setOrganizationId(organizationId);
        latest.setEmployeeId(employeeId);
        latest.setVersionNumber(3);
        latest.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        latest.setEffectiveTo(LocalDate.of(2026, 6, 30));
        CreateSalaryStructureRequest request = request(organizationId, employeeId, LocalDate.of(2026, 7, 1));

        when(repository.countOverlappingEffectiveRanges(
                organizationId, employeeId, request.effectiveFrom(), null)).thenReturn(0L);
        when(repository.findFirstByOrganizationIdAndEmployeeIdOrderByVersionNumberDesc(
                organizationId, employeeId)).thenReturn(Optional.of(latest));
        when(repository.save(any(SalaryStructure.class))).thenAnswer(invocation -> {
            SalaryStructure value = invocation.getArgument(0);
            value.setId(UUID.randomUUID());
            return value;
        });

        var response = service.create(request, actorUserId, false);

        assertEquals(4, response.versionNumber());
        assertEquals(latest.getId(), response.supersedesStructureId());
    }

    private CreateSalaryStructureRequest request(UUID organizationId, UUID employeeId, LocalDate effectiveFrom) {
        return new CreateSalaryStructureRequest(
                organizationId,
                employeeId,
                "INR",
                new BigDecimal("50000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("1000.00"),
                effectiveFrom,
                null,
                true);
    }
}
