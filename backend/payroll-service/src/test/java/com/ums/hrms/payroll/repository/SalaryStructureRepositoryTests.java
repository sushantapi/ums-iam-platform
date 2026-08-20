package com.ums.hrms.payroll.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.ums.hrms.payroll.config.JpaAuditConfig;
import com.ums.hrms.payroll.entity.SalaryStructure;

@DataJpaTest
@Import(JpaAuditConfig.class)
class SalaryStructureRepositoryTests {

    @Autowired
    private SalaryStructureRepository repository;

    @Test
    void detectsInclusiveAndOpenEndedEffectiveRangeOverlap() {
        UUID organizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        repository.saveAndFlush(structure(
                organizationId,
                employeeId,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)));

        assertEquals(1L, repository.countOverlappingEffectiveRanges(
                organizationId,
                employeeId,
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 10)));
        assertEquals(0L, repository.countOverlappingEffectiveRanges(
                organizationId,
                employeeId,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)));
        assertEquals(1L, repository.countOverlappingEffectiveRanges(
                organizationId,
                employeeId,
                LocalDate.of(2026, 8, 15),
                null));
    }

    @Test
    void overlapQueryIsTenantAndEmployeeScoped() {
        UUID organizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        repository.saveAndFlush(structure(
                organizationId,
                employeeId,
                LocalDate.of(2026, 8, 1),
                null));

        assertEquals(0L, repository.countOverlappingEffectiveRanges(
                UUID.randomUUID(), employeeId, LocalDate.of(2026, 8, 1), null));
        assertEquals(0L, repository.countOverlappingEffectiveRanges(
                organizationId, UUID.randomUUID(), LocalDate.of(2026, 8, 1), null));
    }

    private SalaryStructure structure(
            UUID organizationId,
            UUID employeeId,
            LocalDate effectiveFrom,
            LocalDate effectiveTo) {
        SalaryStructure structure = new SalaryStructure();
        structure.setOrganizationId(organizationId);
        structure.setEmployeeId(employeeId);
        structure.setCurrency("INR");
        structure.setBasicPay(new BigDecimal("50000.00"));
        structure.setAllowanceTotal(BigDecimal.ZERO.setScale(2));
        structure.setDeductionTotal(BigDecimal.ZERO.setScale(2));
        structure.setEffectiveFrom(effectiveFrom);
        structure.setEffectiveTo(effectiveTo);
        structure.setActive(true);
        structure.setCreatedBy(UUID.randomUUID());
        return structure;
    }
}
