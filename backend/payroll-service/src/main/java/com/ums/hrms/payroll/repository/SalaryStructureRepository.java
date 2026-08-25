package com.ums.hrms.payroll.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ums.hrms.payroll.entity.SalaryStructure;

import jakarta.persistence.LockModeType;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, UUID> {

    Optional<SalaryStructure> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s
            from SalaryStructure s
            where s.id = :id
              and s.organizationId = :organizationId
            """)
    Optional<SalaryStructure> findByIdAndOrganizationIdForUpdate(
            @Param("id") UUID id,
            @Param("organizationId") UUID organizationId);

    List<SalaryStructure> findAllByOrganizationIdAndEmployeeIdOrderByEffectiveFromDesc(
            UUID organizationId,
            UUID employeeId);

    Optional<SalaryStructure> findFirstByOrganizationIdAndEmployeeIdOrderByVersionNumberDesc(
            UUID organizationId,
            UUID employeeId);

    boolean existsBySupersedesStructureId(UUID supersedesStructureId);

    @Query("""
            select count(s)
            from SalaryStructure s
            where s.organizationId = :organizationId
              and s.employeeId = :employeeId
              and (:effectiveTo is null or s.effectiveFrom <= :effectiveTo)
              and (s.effectiveTo is null or s.effectiveTo >= :effectiveFrom)
            """)
    long countOverlappingEffectiveRanges(
            @Param("organizationId") UUID organizationId,
            @Param("employeeId") UUID employeeId,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo);

    @Query("""
            select count(s)
            from SalaryStructure s
            where s.organizationId = :organizationId
              and s.employeeId = :employeeId
              and s.id <> :excludedId
              and (:effectiveTo is null or s.effectiveFrom <= :effectiveTo)
              and (s.effectiveTo is null or s.effectiveTo >= :effectiveFrom)
            """)
    long countOverlappingEffectiveRangesExcludingId(
            @Param("organizationId") UUID organizationId,
            @Param("employeeId") UUID employeeId,
            @Param("excludedId") UUID excludedId,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo);

    @Query("""
            select s
            from SalaryStructure s
            where s.organizationId = :organizationId
              and s.active = true
              and s.effectiveFrom <= :effectiveOn
              and (s.effectiveTo is null or s.effectiveTo >= :effectiveOn)
            order by s.employeeId
            """)
    List<SalaryStructure> findAllActiveEffectiveOn(
            @Param("organizationId") UUID organizationId,
            @Param("effectiveOn") LocalDate effectiveOn);
}
