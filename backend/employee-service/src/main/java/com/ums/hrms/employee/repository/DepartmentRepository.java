package com.ums.hrms.employee.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.hrms.employee.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Page<Department> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    Optional<Department> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
}
