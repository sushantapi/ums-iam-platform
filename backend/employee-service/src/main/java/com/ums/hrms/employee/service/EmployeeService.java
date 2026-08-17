package com.ums.hrms.employee.service;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.employee.dto.CreateEmployeeRequest;
import com.ums.hrms.employee.dto.EmployeePageResponse;
import com.ums.hrms.employee.dto.EmployeeResponse;
import com.ums.hrms.employee.dto.UpdateEmployeeRequest;
import com.ums.hrms.employee.entity.Employee;
import com.ums.hrms.employee.entity.EmployeeStatus;
import com.ums.hrms.employee.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final OrganizationAccessService organizationAccessService;
    private final EmployeeAuditPublisher employeeAuditPublisher;

    public EmployeeResponse create(CreateEmployeeRequest request, UUID actorUserId, boolean superAdmin) {
        organizationAccessService.assertCanAccess(request.organizationId(), actorUserId, superAdmin);
        organizationAccessService.assertUserBelongsToOrganization(request.organizationId(), request.umsUserId());

        String employeeCode = normalizeEmployeeCode(request.employeeCode());
        if (employeeRepository.existsByOrganizationIdAndEmployeeCodeIgnoreCase(request.organizationId(), employeeCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee code already exists in organization");
        }
        if (employeeRepository.existsByOrganizationIdAndUmsUserId(request.organizationId(), request.umsUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "UMS user is already linked to an employee in organization");
        }

        Employee employee = Employee.builder()
                .organizationId(request.organizationId())
                .umsUserId(request.umsUserId())
                .employeeCode(employeeCode)
                .departmentId(request.departmentId())
                .designationId(request.designationId())
                .status(EmployeeStatus.ACTIVE)
                .build();

        Employee saved = employeeRepository.save(employee);
        employeeAuditPublisher.publishCreated(saved, actorUserId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public EmployeePageResponse list(UUID organizationId, int page, int size, UUID actorUserId, boolean superAdmin) {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);
        validatePage(page, size);

        var employees = employeeRepository.findAllByOrganizationId(
                organizationId,
                PageRequest.of(page, size, Sort.by("employeeCode").ascending()));

        return new EmployeePageResponse(
                employees.getContent().stream().map(this::toResponse).toList(),
                employees.getNumber(),
                employees.getSize(),
                employees.getTotalElements(),
                employees.getTotalPages());
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(UUID employeeId, UUID organizationId, UUID actorUserId, boolean superAdmin) {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);
        return toResponse(findScoped(employeeId, organizationId));
    }

    public EmployeeResponse update(
            UUID employeeId,
            UpdateEmployeeRequest request,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(request.organizationId(), actorUserId, superAdmin);
        Employee employee = findScoped(employeeId, request.organizationId());

        String employeeCode = normalizeEmployeeCode(request.employeeCode());
        if (!employee.getEmployeeCode().equalsIgnoreCase(employeeCode)
                && employeeRepository.existsByOrganizationIdAndEmployeeCodeIgnoreCase(
                        request.organizationId(), employeeCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee code already exists in organization");
        }

        employee.setEmployeeCode(employeeCode);
        employee.setDepartmentId(request.departmentId());
        employee.setDesignationId(request.designationId());
        employee.setStatus(request.status());

        Employee saved = employeeRepository.save(employee);
        employeeAuditPublisher.publishUpdated(saved, actorUserId);
        return toResponse(saved);
    }

    private Employee findScoped(UUID employeeId, UUID organizationId) {
        return employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    private String normalizeEmployeeCode(String employeeCode) {
        return employeeCode.trim();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or size");
        }
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getUmsUserId(),
                employee.getOrganizationId(),
                employee.getEmployeeCode(),
                employee.getDepartmentId(),
                employee.getDesignationId(),
                employee.getStatus(),
                employee.getCreatedAt(),
                employee.getUpdatedAt());
    }
}
