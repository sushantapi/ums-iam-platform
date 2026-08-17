package com.ums.hrms.employee.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.hrms.employee.entity.Designation;

public interface DesignationRepository extends JpaRepository<Designation, UUID> {

    Page<Designation> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    Optional<Designation> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
}
