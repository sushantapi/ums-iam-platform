package com.ums.hrms.employee.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.employee.dto.EmployeeInternalResponse;
import com.ums.hrms.employee.entity.Department;
import com.ums.hrms.employee.entity.Designation;
import com.ums.hrms.employee.entity.Employee;
import com.ums.hrms.employee.repository.DepartmentRepository;
import com.ums.hrms.employee.repository.DesignationRepository;
import com.ums.hrms.employee.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeInternalQueryService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;

    @Transactional(readOnly = true)
    public EmployeeInternalResponse get(UUID employeeId, UUID organizationId) {
        Employee employee = employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        String departmentName = employee.getDepartmentId() == null
                ? null
                : departmentRepository.findByIdAndOrganizationId(employee.getDepartmentId(), organizationId)
                        .map(Department::getName)
                        .orElse(null);

        String designationName = employee.getDesignationId() == null
                ? null
                : designationRepository.findByIdAndOrganizationId(employee.getDesignationId(), organizationId)
                        .map(Designation::getName)
                        .orElse(null);

        return new EmployeeInternalResponse(
                employee.getId(),
                employee.getOrganizationId(),
                employee.getEmployeeCode(),
                employee.getDisplayName(),
                employee.getDateOfJoining(),
                departmentName,
                designationName,
                employee.getPanDisplay(),
                employee.getUanDisplay(),
                employee.getEsiDisplay(),
                employee.getBankAccountDisplay(),
                employee.getStatus());
    }
}
