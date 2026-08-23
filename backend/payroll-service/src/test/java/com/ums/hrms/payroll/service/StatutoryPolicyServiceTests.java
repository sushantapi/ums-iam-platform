package com.ums.hrms.payroll.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
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

import com.ums.hrms.payroll.dto.CreateStatutoryPolicyRequest;
import com.ums.hrms.payroll.entity.StatutoryPolicy;
import com.ums.hrms.payroll.repository.StatutoryPolicyRepository;

@ExtendWith(MockitoExtension.class)
class StatutoryPolicyServiceTests {

    @Mock StatutoryPolicyRepository repository;
    @Mock OrganizationAccessService organizationAccessService;
    @Mock PayrollAuditPublisher payrollAuditPublisher;
    @InjectMocks StatutoryPolicyService service;

    private UUID organizationId;
    private UUID actorUserId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();
    }

    @Test
    void createsNormalizedActivePolicyAndPublishesAudit() {
        CreateStatutoryPolicyRequest request = request(
                " in ",
                "  IN-2026.1  ",
                LocalDate.of(2026, 8, 1),
                null,
                null);

        when(repository.countOverlappingActiveEffectiveRanges(
                organizationId,
                "IN",
                request.effectiveFrom(),
                null))
                .thenReturn(0L);

        when(repository.save(any(StatutoryPolicy.class)))
                .thenAnswer(invocation -> {
                    StatutoryPolicy saved = invocation.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    return saved;
                });

        var response = service.create(request, actorUserId, false);

        verify(organizationAccessService)
                .assertCanAccess(organizationId, actorUserId, false);
        verify(payrollAuditPublisher)
                .publishStatutoryPolicyCreated(any(StatutoryPolicy.class), any());

        assertEquals("IN", response.countryCode());
        assertEquals("IN-2026.1", response.policyVersion());
        assertTrue(response.active());
        assertEquals(new BigDecimal("0.120000"), response.pfEmployeeRate());
        assertEquals(new BigDecimal("15000.00"), response.pfContributionWageCeiling());
        assertEquals(new BigDecimal("0.007500"), response.esiEmployeeRate());
        assertEquals(new BigDecimal("21000.00"), response.esiWageEligibilityCeiling());
        assertEquals(actorUserId, response.createdBy());
    }

    @Test
    void rejectsDuplicatePolicyVersion() {
        CreateStatutoryPolicyRequest request = request(
                " in ",
                "  IN-2026.1  ",
                LocalDate.of(2026, 8, 1),
                null,
                true);

        when(repository.existsByOrganizationIdAndCountryCodeAndPolicyVersion(
                organizationId,
                "IN",
                "IN-2026.1"))
                .thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request, actorUserId, false));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals(
                "Statutory policy version already exists",
                ex.getReason());

        verify(repository, never())
                .countOverlappingActiveEffectiveRanges(
                        any(),
                        any(),
                        any(),
                        nullable(LocalDate.class));
        verify(repository, never()).save(any());
        verify(payrollAuditPublisher, never())
                .publishStatutoryPolicyCreated(any(), any());
    }

    @Test
    void rejectsOverlappingActivePolicyRange() {
        CreateStatutoryPolicyRequest request = request(
                "IN",
                "IN-2026.2",
                LocalDate.of(2026, 9, 1),
                null,
                true);

        when(repository.countOverlappingActiveEffectiveRanges(
                organizationId,
                "IN",
                request.effectiveFrom(),
                null))
                .thenReturn(1L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request, actorUserId, false));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals(
                "Overlapping active statutory policy effective range exists",
                ex.getReason());

        verify(repository, never()).save(any());
        verify(payrollAuditPublisher, never())
                .publishStatutoryPolicyCreated(any(), any());
    }

    @Test
    void inactivePolicyDoesNotRequireOverlapCheck() {
        CreateStatutoryPolicyRequest request = request(
                "IN",
                "IN-DRAFT",
                LocalDate.of(2026, 9, 1),
                null,
                false);

        when(repository.save(any(StatutoryPolicy.class)))
                .thenAnswer(invocation -> {
                    StatutoryPolicy saved = invocation.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    return saved;
                });

        var response = service.create(request, actorUserId, false);

        assertFalse(response.active());

        verify(repository, never())
                .countOverlappingActiveEffectiveRanges(any(), any(), any(), nullable(LocalDate.class));
        verify(payrollAuditPublisher)
                .publishStatutoryPolicyCreated(any(StatutoryPolicy.class), any());
    }

    @Test
    void rejectsUnsupportedCountry() {
        CreateStatutoryPolicyRequest request = request(
                "US",
                "US-2026.1",
                LocalDate.of(2026, 8, 1),
                null,
                true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request, actorUserId, false));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals(
                "countryCode must be IN for the current statutory payroll foundation",
                ex.getReason());

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsInvalidEffectiveDateRange() {
        CreateStatutoryPolicyRequest request = request(
                "IN",
                "IN-2026.1",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 8, 31),
                true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request, actorUserId, false));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals(
                "effectiveTo cannot be before effectiveFrom",
                ex.getReason());

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsRateAboveOne() {
        CreateStatutoryPolicyRequest request =
                new CreateStatutoryPolicyRequest(
                        organizationId,
                        "IN",
                        "IN-INVALID",
                        LocalDate.of(2026, 8, 1),
                        null,
                        true,
                        new BigDecimal("1.000001"),
                        new BigDecimal("0.120000"),
                        new BigDecimal("15000.00"),
                        new BigDecimal("0.007500"),
                        new BigDecimal("0.032500"),
                        new BigDecimal("21000.00"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request, actorUserId, false));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals(
                "pfEmployeeRate must be between 0 and 1",
                ex.getReason());

        verify(repository, never()).save(any());
    }

    @Test
    void listsTenantScopedPoliciesWithNormalizedCountry() {
        StatutoryPolicy policy = policy();

        when(repository
                .findAllByOrganizationIdAndCountryCodeOrderByEffectiveFromDesc(
                        organizationId,
                        "IN"))
                .thenReturn(List.of(policy));

        var result = service.list(
                organizationId,
                " in ",
                actorUserId,
                false);

        verify(organizationAccessService)
                .assertCanAccess(organizationId, actorUserId, false);

        assertEquals(1, result.size());
        assertEquals(policy.getId(), result.getFirst().id());
        assertEquals("IN", result.getFirst().countryCode());
    }

    @Test
    void getsPolicyUsingTenantScopedId() {
        StatutoryPolicy policy = policy();

        when(repository.findByIdAndOrganizationId(
                policy.getId(),
                organizationId))
                .thenReturn(Optional.of(policy));

        var response = service.get(
                policy.getId(),
                organizationId,
                actorUserId,
                false);

        verify(organizationAccessService)
                .assertCanAccess(organizationId, actorUserId, false);

        assertEquals(policy.getId(), response.id());
        assertEquals(organizationId, response.organizationId());
    }

    private CreateStatutoryPolicyRequest request(
            String countryCode,
            String policyVersion,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            Boolean active) {
        return new CreateStatutoryPolicyRequest(
                organizationId,
                countryCode,
                policyVersion,
                effectiveFrom,
                effectiveTo,
                active,
                new BigDecimal("0.120000"),
                new BigDecimal("0.120000"),
                new BigDecimal("15000.00"),
                new BigDecimal("0.007500"),
                new BigDecimal("0.032500"),
                new BigDecimal("21000.00"));
    }

    private StatutoryPolicy policy() {
        StatutoryPolicy policy = new StatutoryPolicy();
        policy.setId(UUID.randomUUID());
        policy.setOrganizationId(organizationId);
        policy.setCountryCode("IN");
        policy.setPolicyVersion("IN-2026.1");
        policy.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        policy.setActive(true);
        policy.setPfEmployeeRate(new BigDecimal("0.120000"));
        policy.setPfEmployerRate(new BigDecimal("0.120000"));
        policy.setPfContributionWageCeiling(new BigDecimal("15000.00"));
        policy.setEsiEmployeeRate(new BigDecimal("0.007500"));
        policy.setEsiEmployerRate(new BigDecimal("0.032500"));
        policy.setEsiWageEligibilityCeiling(new BigDecimal("21000.00"));
        policy.setCreatedBy(actorUserId);
        return policy;
    }
}