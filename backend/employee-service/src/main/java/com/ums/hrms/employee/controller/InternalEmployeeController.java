package com.ums.hrms.employee.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ums.hrms.employee.dto.EmployeeInternalResponse;
import com.ums.hrms.employee.service.EmployeeInternalQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/hrms/employees")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
public class InternalEmployeeController {

    private final EmployeeInternalQueryService employeeInternalQueryService;

    @GetMapping("/{employeeId}")
    public EmployeeInternalResponse get(
            @PathVariable UUID employeeId,
            @RequestParam UUID organizationId) {
        return employeeInternalQueryService.get(employeeId, organizationId);
    }
}
