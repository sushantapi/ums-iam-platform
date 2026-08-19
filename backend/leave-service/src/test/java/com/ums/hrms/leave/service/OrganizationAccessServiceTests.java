package com.ums.hrms.leave.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.leave.client.OrganizationServiceClient;

import feign.FeignException;
import feign.Request;

@ExtendWith(MockitoExtension.class)
class OrganizationAccessServiceTests {

    @Mock OrganizationServiceClient organizationServiceClient;
    @InjectMocks OrganizationAccessService organizationAccessService;

    private final UUID organizationId = UUID.randomUUID();
    private final UUID actorUserId = UUID.randomUUID();

    @Test
    void delegatesActorIdentityAndSuperAdminFlag() {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, true);

        verify(organizationServiceClient).assertAccessible(organizationId, actorUserId, true);
    }

    @Test
    void mapsForbiddenToAccessDenied() {
        when(organizationServiceClient.assertAccessible(organizationId, actorUserId, false))
                .thenThrow(new FeignException.Forbidden("forbidden", request()));

        assertThrows(
                AccessDeniedException.class,
                () -> organizationAccessService.assertCanAccess(organizationId, actorUserId, false));
    }

    @Test
    void mapsNotFoundToOrganizationNotFound() {
        when(organizationServiceClient.assertAccessible(organizationId, actorUserId, false))
                .thenThrow(new FeignException.NotFound("not found", request()));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> organizationAccessService.assertCanAccess(organizationId, actorUserId, false));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void mapsOtherFeignFailuresToBadGateway() {
        when(organizationServiceClient.assertAccessible(organizationId, actorUserId, false))
                .thenThrow(new FeignException.ServiceUnavailable("unavailable", request()));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> organizationAccessService.assertCanAccess(organizationId, actorUserId, false));

        assertEquals(502, ex.getStatusCode().value());
    }

    private Request request() {
        return Request.create(Request.HttpMethod.GET, "/api/v1/internal/organizations/" + organizationId,
                java.util.Map.of(), null, null, null);
    }
}
