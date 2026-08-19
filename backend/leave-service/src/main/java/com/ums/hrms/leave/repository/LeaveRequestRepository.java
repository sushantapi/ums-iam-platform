package com.ums.hrms.leave.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ums.hrms.leave.entity.LeaveRequest;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    Page<LeaveRequest> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    Optional<LeaveRequest> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("""
            select count(l) > 0
            from LeaveRequest l
            where l.organizationId = :organizationId
              and l.employeeId = :employeeId
              and l.status in (com.ums.hrms.leave.entity.LeaveStatus.PENDING,
                               com.ums.hrms.leave.entity.LeaveStatus.APPROVED)
              and l.startDate <= :endDate
              and l.endDate >= :startDate
            """)
    boolean existsOverlappingActiveRequest(
            @Param("organizationId") UUID organizationId,
            @Param("employeeId") UUID employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
