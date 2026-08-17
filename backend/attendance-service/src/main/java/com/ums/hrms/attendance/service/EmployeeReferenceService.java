package com.ums.hrms.attendance.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.attendance.client.EmployeeServiceClient;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeReferenceService {

    private final EmployeeServiceClient employeeServiceClient;

    public void assertActiveEmployee(UUID organizationId, UUID employeeId) {
        try {
            EmployeeServiceClient.EmployeeSummary employee = employeeServiceClient.getEmployee(employeeId, organizationId);
            if (!organizationId.equals(employee.organizationId())
                    || !employeeId.equals(employee.id())
                    || !"ACTIVE".equals(employee.status())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Employee not found or inactive in organization");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Employee not found or inactive in organization",
                    ex);
        } catch (FeignException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Employee service unavailable", ex);
        }
    }
}
