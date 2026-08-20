package com.ums.hrms.payroll.repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ums.hrms.payroll.entity.PayrollRun;

import jakarta.persistence.LockModeType;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {

    Optional<PayrollRun> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<PayrollRun> findAllByOrganizationIdOrderByPayrollMonthDesc(UUID organizationId);

    boolean existsByOrganizationIdAndPayrollMonth(UUID organizationId, YearMonth payrollMonth);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r
            from PayrollRun r
            where r.id = :id
              and r.organizationId = :organizationId
            """)
    Optional<PayrollRun> findByIdAndOrganizationIdForUpdate(
            @Param("id") UUID id,
            @Param("organizationId") UUID organizationId);
}
