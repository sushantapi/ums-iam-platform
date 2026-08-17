package com.ums.hrms.employee.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.employee.client.OrganizationServiceClient;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationAccessService {

    private final OrganizationServiceClient organizationServiceClient;

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
}
