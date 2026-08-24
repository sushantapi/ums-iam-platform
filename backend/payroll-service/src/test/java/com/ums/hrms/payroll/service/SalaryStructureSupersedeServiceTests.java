package com.ums.hrms.payroll.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.payroll.dto.SupersedeSalaryStructureRequest;
import com.ums.hrms.payroll.entity.SalaryStructure;
import com.ums.hrms.payroll.entity.TaxRegime;
import com.ums.hrms.payroll.repository.SalaryStructureRepository;

@ExtendWith(MockitoExtension.class)
class SalaryStructureSupersedeServiceTests {

    @Mock SalaryStructureRepository repository;
    @Mock OrganizationAccessService organizationAccessService;
    @Mock PayrollTenantValidationService employeeValidationService;
    @Mock PayrollAuditPublisher payrollAuditPublisher;
    @InjectMocks SalaryStructureService service;

    private UUID organizationId;
    private UUID employeeId;
    private UUID actorUserId;
    private SalaryStructure predecessor;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();

        predecessor = new SalaryStructure();
        predecessor.setId(UUID.randomUUID());
        predecessor.setOrganizationId(organizationId);
        predecessor.setEmployeeId(employeeId);
        predecessor.setVersionNumber(1);
        predecessor.setCurrency("INR");
        predecessor.setBasicPay(new BigDecimal("50000.00"));
        predecessor.setAllowanceTotal(new BigDecimal("5000.00"));
        predecessor.setDeductionTotal(new BigDecimal("1000.00"));
        predecessor.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        predecessor.setActive(true);
        predecessor.setCreatedBy(actorUserId);
    }

    @Test
    void supersedesAtomicallyAndRetainsHistoricalPredecessor() {
        SupersedeSalaryStructureRequest request = request(LocalDate.of(2026, 7, 1));
        when(repository.findByIdAndOrganizationIdForUpdate(predecessor.getId(), organizationId))
                .thenReturn(Optional.of(predecessor));
        when(repository.existsBySupersedesStructureId(predecessor.getId())).thenReturn(false);
        when(repository.countOverlappingEffectiveRangesExcludingId(
                organizationId,
                employeeId,
                predecessor.getId(),
                request.effectiveFrom(),
                null)).thenReturn(0L);
        when(repository.save(any(SalaryStructure.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.saveAndFlush(any(SalaryStructure.class))).thenAnswer(invocation -> {
            SalaryStructure successor = invocation.getArgument(0);
            successor.setId(UUID.randomUUID());
            return successor;
        });

        var response = service.supersede(predecessor.getId(), request, actorUserId, false);

        assertEquals(LocalDate.of(2026, 6, 30), predecessor.getEffectiveTo());
        assertEquals(actorUserId, predecessor.getSupersededBy());
        assertEquals(true, predecessor.isActive());
        assertEquals(2, response.versionNumber());
        assertEquals(predecessor.getId(), response.supersedesStructureId());
        assertEquals(LocalDate.of(2026, 7, 1), response.effectiveFrom());
        assertNull(response.effectiveTo());
        assertEquals(new BigDecimal("60000.00"), response.basicPay());
        verify(employeeValidationService).validateActiveEmployee(employeeId, organizationId);
        verify(payrollAuditPublisher).publishSalaryStructureSuperseded(
                org.mockito.ArgumentMatchers.eq(predecessor),
                any(SalaryStructure.class),
                org.mockito.ArgumentMatchers.eq(actorUserId));
    }

    @Test
    void successorInheritsPredecessorOriginalEndDate() {
        predecessor.setEffectiveTo(LocalDate.of(2026, 12, 31));
        SupersedeSalaryStructureRequest request = request(LocalDate.of(2026, 7, 1));
        when(repository.findByIdAndOrganizationIdForUpdate(predecessor.getId(), organizationId))
                .thenReturn(Optional.of(predecessor));
        when(repository.countOverlappingEffectiveRangesExcludingId(
                organizationId,
                employeeId,
                predecessor.getId(),
                request.effectiveFrom(),
                LocalDate.of(2026, 12, 31))).thenReturn(0L);
        when(repository.save(any(SalaryStructure.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.saveAndFlush(any(SalaryStructure.class))).thenAnswer(invocation -> {
            SalaryStructure successor = invocation.getArgument(0);
            successor.setId(UUID.randomUUID());
            return successor;
        });

        var response = service.supersede(predecessor.getId(), request, actorUserId, false);

        assertEquals(LocalDate.of(2026, 6, 30), predecessor.getEffectiveTo());
        assertEquals(LocalDate.of(2026, 12, 31), response.effectiveTo());
    }

    @Test
    void rejectsSecondSupersedeOfSamePredecessor() {
        when(repository.findByIdAndOrganizationIdForUpdate(predecessor.getId(), organizationId))
                .thenReturn(Optional.of(predecessor));
        when(repository.existsBySupersedesStructureId(predecessor.getId())).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.supersede(
                        predecessor.getId(),
                        request(LocalDate.of(2026, 7, 1)),
                        actorUserId,
                        false));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals("Salary structure has already been superseded", ex.getReason());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsEqualOrEarlierSuccessorEffectiveDate() {
        when(repository.findByIdAndOrganizationIdForUpdate(predecessor.getId(), organizationId))
                .thenReturn(Optional.of(predecessor));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.supersede(
                        predecessor.getId(),
                        request(predecessor.getEffectiveFrom()),
                        actorUserId,
                        false));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Successor effectiveFrom must be after predecessor effectiveFrom", ex.getReason());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsSuccessorOverlapWithAnotherVersion() {
        SupersedeSalaryStructureRequest request = request(LocalDate.of(2026, 7, 1));
        when(repository.findByIdAndOrganizationIdForUpdate(predecessor.getId(), organizationId))
                .thenReturn(Optional.of(predecessor));
        when(repository.countOverlappingEffectiveRangesExcludingId(
                organizationId,
                employeeId,
                predecessor.getId(),
                request.effectiveFrom(),
                null)).thenReturn(1L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.supersede(predecessor.getId(), request, actorUserId, false));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals("Successor salary structure would overlap another salary version", ex.getReason());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void locksPredecessorWithinRequestedTenant() {
        UUID otherOrganizationId = UUID.randomUUID();
        SupersedeSalaryStructureRequest request = new SupersedeSalaryStructureRequest(
                otherOrganizationId,
                "INR",
                new BigDecimal("60000.00"),
                new BigDecimal("7000.00"),
                new BigDecimal("1200.00"),
                true,
                new BigDecimal("15000.00"),
                false,
                null,
                new BigDecimal("500.00"),
                TaxRegime.NEW,
                LocalDate.of(2026, 7, 1));
        when(repository.findByIdAndOrganizationIdForUpdate(predecessor.getId(), otherOrganizationId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.supersede(predecessor.getId(), request, actorUserId, false));

        assertEquals(404, ex.getStatusCode().value());
        verify(organizationAccessService).assertCanAccess(otherOrganizationId, actorUserId, false);
        verify(employeeValidationService, never()).validateActiveEmployee(any(), any());
    }

    private SupersedeSalaryStructureRequest request(LocalDate effectiveFrom) {
        return new SupersedeSalaryStructureRequest(
                organizationId,
                "INR",
                new BigDecimal("60000.00"),
                new BigDecimal("7000.00"),
                new BigDecimal("1200.00"),
                true,
                new BigDecimal("15000.00"),
                false,
                null,
                new BigDecimal("500.00"),
                TaxRegime.NEW,
                effectiveFrom);
    }
}
