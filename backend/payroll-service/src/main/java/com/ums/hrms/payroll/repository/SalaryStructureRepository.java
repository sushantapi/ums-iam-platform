package com.ums.hrms.payroll.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.hrms.payroll.entity.SalaryStructure;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, UUID> {

    Optional<SalaryStructure> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<SalaryStructure> findAllByOrganizationIdAndEmployeeIdOrderByEffectiveFromDesc(
            UUID organizationId,
            UUID employeeId);
}
