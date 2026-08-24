package com.ums.hrms.payroll.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.payroll.client.OrganizationServiceClient;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;

@ExtendWith(MockitoExtension.class)
class OrganizationAccessServiceTests {

    @Mock OrganizationServiceClient organizationServiceClient;
    @Mock PayrollTenantValidationService payrollTenantValidationService;
    @InjectMocks OrganizationAccessService organizationAccessService;

    private final UUID organizationId = UUID.randomUUID();
    private final UUID actorUserId = UUID.randomUUID();

    @Test
    void delegatesActorIdentityAndSuperAdminFlag() {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, true);
        verify(organizationServiceClient).assertAccessible(organizationId, actorUserId, true);
    }

    @Test
    void returnsImmutableLogoAssetBytes() {
        UUID assetId = UUID.randomUUID();
        byte[] logo = new byte[] {1, 2, 3, 4};
        when(organizationServiceClient.getLogoAsset(organizationId, assetId)).thenReturn(logo);

        assertArrayEquals(logo, organizationAccessService.getLogoAsset(organizationId, assetId));
        verify(organizationServiceClient).getLogoAsset(organizationId, assetId);
    }

    @Test
    void rejectsEmptyLogoAssetResponse() {
        UUID assetId = UUID.randomUUID();
        when(organizationServiceClient.getLogoAsset(organizationId, assetId)).thenReturn(new byte[0]);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> organizationAccessService.getLogoAsset(organizationId, assetId));

        assertEquals(502, ex.getStatusCode().value());
    }

    @Test
    void mapsForbiddenToAccessDenied() {
        doThrow(new FeignException.Forbidden("forbidden", request(), null, Map.of()))
                .when(organizationServiceClient)
                .assertAccessible(organizationId, actorUserId, false);

        assertThrows(
                AccessDeniedException.class,
                () -> organizationAccessService.assertCanAccess(organizationId, actorUserId, false));
    }

    @Test
    void mapsNotFoundToOrganizationNotFound() {
        doThrow(new FeignException.NotFound("not found", request(), null, Map.of()))
                .when(organizationServiceClient)
                .assertAccessible(organizationId, actorUserId, false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> organizationAccessService.assertCanAccess(organizationId, actorUserId, false));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void mapsOtherFeignFailuresToBadGateway() {
        doThrow(new FeignException.ServiceUnavailable("unavailable", request(), null, Map.of()))
                .when(organizationServiceClient)
                .assertAccessible(organizationId, actorUserId, false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> organizationAccessService.assertCanAccess(organizationId, actorUserId, false));

        assertEquals(502, ex.getStatusCode().value());
    }

    private Request request() {
        return Request.create(
                Request.HttpMethod.GET,
                "/api/v1/internal/organizations/" + organizationId,
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate());
    }
}
