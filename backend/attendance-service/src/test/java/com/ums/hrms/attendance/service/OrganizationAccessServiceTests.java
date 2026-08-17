package com.ums.hrms.attendance.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.ums.hrms.attendance.client.OrganizationServiceClient;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;

@ExtendWith(MockitoExtension.class)
class OrganizationAccessServiceTests {

    @Mock OrganizationServiceClient organizationServiceClient;
    @InjectMocks OrganizationAccessService organizationAccessService;

    @Test
    void acceptsAccessibleOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        assertDoesNotThrow(() -> organizationAccessService.assertCanAccess(organizationId, actorUserId, false));
    }

    @Test
    void mapsForbiddenToAccessDenied() {
        UUID organizationId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/api/v1/internal/organizations/" + organizationId,
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate());
        doThrow(new FeignException.Forbidden("forbidden", request, null, Map.of()))
                .when(organizationServiceClient)
                .assertAccessible(organizationId, actorUserId, false);

        assertThrows(
                AccessDeniedException.class,
                () -> organizationAccessService.assertCanAccess(organizationId, actorUserId, false));
    }
}
