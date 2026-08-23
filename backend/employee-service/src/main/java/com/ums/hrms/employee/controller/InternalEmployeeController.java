package com.ums.hrms.employee.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.employee.dto.EmployeeInternalResponse;
import com.ums.hrms.employee.entity.Employee;
import com.ums.hrms.employee.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/hrms/employees")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
public class InternalEmployeeController {

    private final EmployeeRepository employeeRepository;

    @GetMapping("/{employeeId}")
    public EmployeeInternalResponse get(
            @PathVariable UUID employeeId,
            @RequestParam UUID organizationId) {
        Employee employee = employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        return new EmployeeInternalResponse(
                employee.getId(),
                employee.getOrganizationId(),
                employee.getEmployeeCode(),
                employee.getStatus());
    }
}
