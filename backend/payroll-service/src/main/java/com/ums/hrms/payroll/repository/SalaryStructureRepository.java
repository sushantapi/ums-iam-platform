package com.ums.hrms.payroll.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ums.hrms.payroll.entity.SalaryStructure;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, UUID> {

    Optional<SalaryStructure> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<SalaryStructure> findAllByOrganizationIdAndEmployeeIdOrderByEffectiveFromDesc(
            UUID organizationId,
            UUID employeeId);

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
}
