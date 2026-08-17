package com.ums.hrms.employee.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.employee.entity.MasterDataStatus;
import com.ums.hrms.employee.repository.DepartmentRepository;
import com.ums.hrms.employee.repository.DesignationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationStructureReferenceService {

    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;

    public void validateActiveReferences(UUID organizationId, UUID departmentId, UUID designationId) {
        if (departmentId != null) {
            boolean validDepartment = departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
                    .filter(department -> department.getStatus() == MasterDataStatus.ACTIVE)
                    .isPresent();
            if (!validDepartment) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Department not found or inactive in organization");
            }
        }

        if (designationId != null) {
            boolean validDesignation = designationRepository.findByIdAndOrganizationId(designationId, organizationId)
                    .filter(designation -> designation.getStatus() == MasterDataStatus.ACTIVE)
                    .isPresent();
            if (!validDesignation) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Designation not found or inactive in organization");
            }
        }
    }
}
