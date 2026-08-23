package com.ums.hrms.payroll.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.ums.hrms.payroll.config.JpaAuditConfig;
import com.ums.hrms.payroll.entity.StatutoryPolicy;

@DataJpaTest
@Import(JpaAuditConfig.class)
class StatutoryPolicyRepositoryTests {

    @Autowired
    private StatutoryPolicyRepository repository;

    @Test
    void resolvesOnlyActiveTenantCountryPolicyEffectiveOnDate() {
        UUID organizationId = UUID.randomUUID();
        LocalDate effectiveOn = LocalDate.of(2026, 8, 31);

        StatutoryPolicy expected = repository.saveAndFlush(policy(
                organizationId,
                "IN",
                "IN-2026.1",
                LocalDate.of(2026, 4, 1),
                null,
                true));

        repository.saveAndFlush(policy(
                organizationId,
                "IN",
                "IN-INACTIVE",
                LocalDate.of(2026, 1, 1),
                null,
                false));

        repository.saveAndFlush(policy(
                organizationId,
                "US",
                "US-2026.1",
                LocalDate.of(2026, 1, 1),
                null,
                true));

        repository.saveAndFlush(policy(
                UUID.randomUUID(),
                "IN",
                "OTHER-TENANT",
                LocalDate.of(2026, 1, 1),
                null,
                true));

        repository.saveAndFlush(policy(
                organizationId,
                "IN",
                "IN-FUTURE",
                LocalDate.of(2026, 9, 1),
                null,
                true));

        var policies = repository.findAllActiveEffectiveOn(
                organizationId,
                "IN",
                effectiveOn);

        assertEquals(1, policies.size());
        assertEquals(expected.getId(), policies.getFirst().getId());
        assertEquals("IN-2026.1", policies.getFirst().getPolicyVersion());
    }

    @Test
    void detectsInclusiveAndOpenEndedActivePolicyOverlap() {
        UUID organizationId = UUID.randomUUID();

        repository.saveAndFlush(policy(
                organizationId,
                "IN",
                "IN-AUG-2026",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                true));

        assertEquals(
                1L,
                repository.countOverlappingActiveEffectiveRanges(
                        organizationId,
                        "IN",
                        LocalDate.of(2026, 8, 31),
                        LocalDate.of(2026, 9, 10)));

        assertEquals(
                0L,
                repository.countOverlappingActiveEffectiveRanges(
                        organizationId,
                        "IN",
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30)));

        assertEquals(
                1L,
                repository.countOverlappingActiveEffectiveRanges(
                        organizationId,
                        "IN",
                        LocalDate.of(2026, 8, 15),
                        null));

        repository.saveAndFlush(policy(
                organizationId,
                "IN",
                "IN-INACTIVE-OVERLAP",
                LocalDate.of(2026, 8, 1),
                null,
                false));

        assertEquals(
                1L,
                repository.countOverlappingActiveEffectiveRanges(
                        organizationId,
                        "IN",
                        LocalDate.of(2026, 8, 15),
                        null));

        assertEquals(
                0L,
                repository.countOverlappingActiveEffectiveRanges(
                        UUID.randomUUID(),
                        "IN",
                        LocalDate.of(2026, 8, 15),
                        null));

        assertEquals(
                0L,
                repository.countOverlappingActiveEffectiveRanges(
                        organizationId,
                        "US",
                        LocalDate.of(2026, 8, 15),
                        null));
    }

    @Test
    void duplicateVersionLookupIsTenantAndCountryScoped() {
        UUID organizationId = UUID.randomUUID();

        repository.saveAndFlush(policy(
                organizationId,
                "IN",
                "IN-2026.1",
                LocalDate.of(2026, 4, 1),
                null,
                true));

        assertTrue(
                repository.existsByOrganizationIdAndCountryCodeAndPolicyVersion(
                        organizationId,
                        "IN",
                        "IN-2026.1"));

        assertFalse(
                repository.existsByOrganizationIdAndCountryCodeAndPolicyVersion(
                        UUID.randomUUID(),
                        "IN",
                        "IN-2026.1"));

        assertFalse(
                repository.existsByOrganizationIdAndCountryCodeAndPolicyVersion(
                        organizationId,
                        "US",
                        "IN-2026.1"));

        assertFalse(
                repository.existsByOrganizationIdAndCountryCodeAndPolicyVersion(
                        organizationId,
                        "IN",
                        "IN-2026.2"));
    }

    private StatutoryPolicy policy(
            UUID organizationId,
            String countryCode,
            String policyVersion,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            boolean active) {
        StatutoryPolicy policy = new StatutoryPolicy();
        policy.setOrganizationId(organizationId);
        policy.setCountryCode(countryCode);
        policy.setPolicyVersion(policyVersion);
        policy.setEffectiveFrom(effectiveFrom);
        policy.setEffectiveTo(effectiveTo);
        policy.setActive(active);

        policy.setPfEmployeeRate(new BigDecimal("0.120000"));
        policy.setPfEmployerRate(new BigDecimal("0.120000"));
        policy.setPfContributionWageCeiling(new BigDecimal("15000.00"));

        policy.setEsiEmployeeRate(new BigDecimal("0.007500"));
        policy.setEsiEmployerRate(new BigDecimal("0.032500"));
        policy.setEsiWageEligibilityCeiling(new BigDecimal("21000.00"));

        policy.setCreatedBy(UUID.randomUUID());

        return policy;
    }
}
