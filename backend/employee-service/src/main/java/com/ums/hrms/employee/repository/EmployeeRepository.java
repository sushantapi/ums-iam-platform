package com.ums.hrms.employee.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.hrms.employee.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Page<Employee> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    Optional<Employee> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndEmployeeCodeIgnoreCase(UUID organizationId, String employeeCode);

    boolean existsByOrganizationIdAndUmsUserId(UUID organizationId, UUID umsUserId);
}
