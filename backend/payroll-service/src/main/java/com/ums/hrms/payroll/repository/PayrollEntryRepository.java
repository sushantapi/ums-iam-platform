package com.ums.hrms.payroll.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.hrms.payroll.entity.PayrollEntry;

public interface PayrollEntryRepository extends JpaRepository<PayrollEntry, UUID> {

    List<PayrollEntry> findAllByPayrollRunIdAndOrganizationIdOrderByEmployeeId(
            UUID payrollRunId,
            UUID organizationId);

    Optional<PayrollEntry> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
