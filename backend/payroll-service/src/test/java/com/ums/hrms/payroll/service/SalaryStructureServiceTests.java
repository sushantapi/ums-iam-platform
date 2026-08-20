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
import com.ums.hrms.payroll.repository.SalaryStructureRepository;

@ExtendWith(MockitoExtension.class)
class SalaryStructureServiceTests {

    @Mock SalaryStructureRepository repository;
    @Mock OrganizationAccessService organizationAccessService;
    @Mock PayrollTenantValidationService employeeValidationService;
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
    void createsValidatedSalaryStructureWithDefaults() {
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
        assertEquals("INR", response.currency());
        assertEquals(actorUserId, response.createdBy());
        assertEquals(new BigDecimal("50000.00"), response.basicPay());
        assertEquals(true, response.active());
    }

    @Test
    void rejectsNegativeMoneyBeforePersistence() {
        CreateSalaryStructureRequest request = request(
                "INR",
                new BigDecimal("-1.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalDate.of(2026, 8, 1),
                null,
                true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request, actorUserId, false));

        assertEquals(400, ex.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsInvalidEffectiveDateRange() {
        CreateSalaryStructureRequest request = request(
                "INR",
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 8, 31),
                true);

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
                "INR",
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                true);
        when(repository.countOverlappingEffectiveRanges(
                organizationId,
                employeeId,
                request.effectiveFrom(),
                request.effectiveTo())).thenReturn(1L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request, actorUserId, false));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals("Overlapping salary structure effective range exists", ex.getReason());
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
