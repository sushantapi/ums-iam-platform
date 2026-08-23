package com.ums.hrms.payroll.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ums.hrms.payroll.entity.StatutoryPolicy;

public interface StatutoryPolicyRepository extends JpaRepository<StatutoryPolicy, UUID> {

    Optional<StatutoryPolicy> findByIdAndOrganizationId(
            UUID id,
            UUID organizationId);

    boolean existsByOrganizationIdAndCountryCodeAndPolicyVersion(
            UUID organizationId,
            String countryCode,
            String policyVersion);

    List<StatutoryPolicy> findAllByOrganizationIdAndCountryCodeOrderByEffectiveFromDesc(
            UUID organizationId,
            String countryCode);

    @Query("""
            select count(p)
            from StatutoryPolicy p
            where p.organizationId = :organizationId
              and p.countryCode = :countryCode
              and p.active = true
              and (:effectiveTo is null or p.effectiveFrom <= :effectiveTo)
              and (p.effectiveTo is null or p.effectiveTo >= :effectiveFrom)
            """)
    long countOverlappingActiveEffectiveRanges(
            @Param("organizationId") UUID organizationId,
            @Param("countryCode") String countryCode,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo);

    @Query("""
            select p
            from StatutoryPolicy p
            where p.organizationId = :organizationId
              and p.countryCode = :countryCode
              and p.active = true
              and p.effectiveFrom <= :effectiveOn
              and (p.effectiveTo is null or p.effectiveTo >= :effectiveOn)
            order by p.effectiveFrom desc
            """)
    List<StatutoryPolicy> findAllActiveEffectiveOn(
            @Param("organizationId") UUID organizationId,
            @Param("countryCode") String countryCode,
            @Param("effectiveOn") LocalDate effectiveOn);
}
