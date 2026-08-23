package com.ums.hrms.payroll.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.payroll.dto.CreateSalaryStructureRequest;
import com.ums.hrms.payroll.entity.SalaryStructure;
import com.ums.hrms.payroll.entity.TaxRegime;
import com.ums.hrms.payroll.repository.SalaryStructureRepository;

@ExtendWith(MockitoExtension.class)
class SalaryStructureServiceTests {

    @Mock SalaryStructureRepository repository;
    @Mock OrganizationAccessService organizationAccessService;
    @Mock PayrollTenantValidationService employeeValidationService;
    @Mock PayrollAuditPublisher payrollAuditPublisher;
    @InjectMocks SalaryStructureService service;

    private UUID organizationId;
    private UUID employeeId;
    private UUID actorUserId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();
    }

    @Test
    void createsValidatedSalaryStructureWithDefaultsAndAudit() {
        CreateSalaryStructureRequest request = request(
                null,
                new BigDecimal("50000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("2500.00"),
                LocalDate.of(2026, 8, 1),
                null,
                null);
        when(repository.countOverlappingEffectiveRanges(
                organizationId, employeeId, request.effectiveFrom(), null)).thenReturn(0L);
        when(repository.save(any(SalaryStructure.class))).thenAnswer(invocation -> {
            SalaryStructure saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        var response = service.create(request, actorUserId, false);

        verify(organizationAccessService).assertCanAccess(organizationId, actorUserId, false);
        verify(employeeValidationService).validateActiveEmployee(employeeId, organizationId);
        verify(payrollAuditPublisher).publishSalaryStructureCreated(any(SalaryStructure.class), org.mockito.ArgumentMatchers.eq(actorUserId));
        assertEquals("INR", response.currency());
        assertEquals(actorUserId, response.createdBy());
        assertEquals(new BigDecimal("50000.00"), response.basicPay());
        assertEquals(true, response.active());
    }

    @Test
    void rejectsNegativeMoneyBeforePersistence() {
        CreateSalaryStructureRequest request = request(
                "INR", new BigDecimal("-1.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.of(2026, 8, 1), null, true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request, actorUserId, false));

        assertEquals(400, ex.getStatusCode().value());
        verify(repository, never()).save(any());
        verify(payrollAuditPublisher, never()).publishSalaryStructureCreated(any(), any());
    }

    @Test
    void rejectsInvalidEffectiveDateRange() {
        CreateSalaryStructureRequest request = request(
                "INR", new BigDecimal("50000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 8, 31), true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request, actorUserId, false));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("effectiveTo cannot be before effectiveFrom", ex.getReason());
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsOverlappingEffectiveRange() {
        CreateSalaryStructureRequest request = request(
                "INR", new BigDecimal("50000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), true);
        when(repository.countOverlappingEffectiveRanges(
                organizationId, employeeId, request.effectiveFrom(), request.effectiveTo())).thenReturn(1L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request, actorUserId, false));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals("Overlapping salary structure effective range exists", ex.getReason());
        verify(repository, never()).save(any());
    }

    @Test
    void createsStatutorySalaryInputsAndMapsResponse() {
        CreateSalaryStructureRequest request = new CreateSalaryStructureRequest(
                organizationId,
                employeeId,
                "INR",
                new BigDecimal("50000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("2500.00"),
                true,
                new BigDecimal("15000.00"),
                true,
                new BigDecimal("18000.00"),
                new BigDecimal("1250.00"),
                TaxRegime.NEW,
                LocalDate.of(2026, 8, 1),
                null,
                true);

        when(repository.countOverlappingEffectiveRanges(
                organizationId, employeeId, request.effectiveFrom(), null)).thenReturn(0L);
        when(repository.save(any(SalaryStructure.class))).thenAnswer(invocation -> {
            SalaryStructure saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        var response = service.create(request, actorUserId, false);

        assertEquals(true, response.pfApplicable());
        assertEquals(new BigDecimal("15000.00"), response.pfContributionWage());
        assertEquals(true, response.esiApplicable());
        assertEquals(new BigDecimal("18000.00"), response.esiContributionWage());
        assertEquals(new BigDecimal("1250.00"), response.tdsAmount());
        assertEquals(TaxRegime.NEW, response.taxRegime());
    }

    @Test
    void rejectsMissingPfContributionWageWhenPfApplicable() {
        CreateSalaryStructureRequest request = new CreateSalaryStructureRequest(
                organizationId,
                employeeId,
                "INR",
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true,
                null,
                false,
                null,
                BigDecimal.ZERO,
                null,
                LocalDate.of(2026, 8, 1),
                null,
                true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request, actorUserId, false));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("pfContributionWage is required when PF is applicable", ex.getReason());
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsMissingEsiContributionWageWhenEsiApplicable() {
        CreateSalaryStructureRequest request = new CreateSalaryStructureRequest(
                organizationId,
                employeeId,
                "INR",
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false,
                null,
                true,
                null,
                BigDecimal.ZERO,
                null,
                LocalDate.of(2026, 8, 1),
                null,
                true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request, actorUserId, false));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("esiContributionWage is required when ESI is applicable", ex.getReason());
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsNegativeTdsAmount() {
        CreateSalaryStructureRequest request = new CreateSalaryStructureRequest(
                organizationId,
                employeeId,
                "INR",
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false,
                null,
                false,
                null,
                new BigDecimal("-1.00"),
                null,
                LocalDate.of(2026, 8, 1),
                null,
                true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request, actorUserId, false));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("tdsAmount cannot be negative", ex.getReason());
        verify(repository, never()).save(any());
    }

    @Test
    void listsOnlyRequestedTenantAndEmployee() {
        SalaryStructure structure = structure();
        when(repository.findAllByOrganizationIdAndEmployeeIdOrderByEffectiveFromDesc(organizationId, employeeId))
                .thenReturn(List.of(structure));

        var result = service.list(organizationId, employeeId, actorUserId, false);

        verify(organizationAccessService).assertCanAccess(organizationId, actorUserId, false);
        assertEquals(1, result.size());
        assertEquals(employeeId, result.getFirst().employeeId());
    }

    @Test
    void getsSalaryStructureByTenantScopedId() {
        SalaryStructure structure = structure();
        when(repository.findByIdAndOrganizationId(structure.getId(), organizationId))
                .thenReturn(Optional.of(structure));

        var result = service.get(structure.getId(), organizationId, actorUserId, false);

        verify(organizationAccessService).assertCanAccess(organizationId, actorUserId, false);
        assertEquals(structure.getId(), result.id());
    }

    private CreateSalaryStructureRequest request(
            String currency,
            BigDecimal basicPay,
            BigDecimal allowanceTotal,
            BigDecimal deductionTotal,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            Boolean active) {
        return new CreateSalaryStructureRequest(
                organizationId,
                employeeId,
                currency,
                basicPay,
                allowanceTotal,
                deductionTotal,
                effectiveFrom,
                effectiveTo,
                active);
    }

    private SalaryStructure structure() {
        SalaryStructure structure = new SalaryStructure();
        structure.setId(UUID.randomUUID());
        structure.setOrganizationId(organizationId);
        structure.setEmployeeId(employeeId);
        structure.setCurrency("INR");
        structure.setBasicPay(new BigDecimal("50000.00"));
        structure.setAllowanceTotal(new BigDecimal("5000.00"));
        structure.setDeductionTotal(new BigDecimal("2500.00"));
        structure.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        structure.setActive(true);
        structure.setCreatedBy(actorUserId);
        return structure;
    }
}
