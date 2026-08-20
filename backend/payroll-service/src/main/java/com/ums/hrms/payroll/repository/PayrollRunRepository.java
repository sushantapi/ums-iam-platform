package com.ums.hrms.payroll.repository;

import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.hrms.payroll.entity.PayrollRun;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {

    Optional<PayrollRun> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndPayrollMonth(UUID organizationId, YearMonth payrollMonth);
}
