package com.ums.hrms.attendance.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.hrms.attendance.entity.AttendanceRecord;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, UUID> {

    Page<AttendanceRecord> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    Optional<AttendanceRecord> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndEmployeeIdAndWorkDate(
            UUID organizationId,
            UUID employeeId,
            LocalDate workDate);
}
