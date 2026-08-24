package com.ums.hrms.payroll.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.payroll.client.EmployeeInternalResponse;
import com.ums.hrms.payroll.client.OrganizationProfileInternalResponse;
import com.ums.hrms.payroll.client.OrganizationServiceClient;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationAccessService {

    private final OrganizationServiceClient organizationServiceClient;
    private final PayrollTenantValidationService payrollTenantValidationService;

    public void assertCanAccess(UUID organizationId, UUID actorUserId, boolean superAdmin) {
        try {
            organizationServiceClient.assertAccessible(organizationId, actorUserId, superAdmin);
        } catch (FeignException.Forbidden ex) {
            throw new AccessDeniedException("Organization access denied", ex);
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found", ex);
        } catch (FeignException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Organization service unavailable", ex);
        }
    }

    public OrganizationProfileInternalResponse getProfile(UUID organizationId) {
        OrganizationProfileInternalResponse profile;
        try {
            profile = organizationServiceClient.getProfile(organizationId);
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization profile not found", ex);
        } catch (FeignException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Organization service unavailable", ex);
        }

        if (profile == null || !organizationId.equals(profile.organizationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Organization profile response is invalid");
        }
        return profile;
    }

    public EmployeeInternalResponse getEmployeePresentation(UUID employeeId, UUID organizationId) {
        return payrollTenantValidationService.getEmployee(employeeId, organizationId);
    }
}
