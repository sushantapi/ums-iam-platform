package com.ums.hrms.employee.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.employee.client.OrganizationServiceClient;
import com.ums.hrms.employee.client.OrganizationServiceClient.OrganizationSummary;

@ExtendWith(MockitoExtension.class)
class OrganizationAccessServiceTests {

    @Mock
    private OrganizationServiceClient organizationServiceClient;

    @InjectMocks
    private OrganizationAccessService organizationAccessService;

    @Test
    void acceptsUmsUserWhoBelongsToOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID umsUserId = UUID.randomUUID();

        when(organizationServiceClient.findOrganizationsForUser(umsUserId))
                .thenReturn(List.of(new OrganizationSummary(organizationId)));

        assertDoesNotThrow(
                () -> organizationAccessService.assertUserBelongsToOrganization(organizationId, umsUserId));
    }

    @Test
    void rejectsUmsUserWhoDoesNotBelongToOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID umsUserId = UUID.randomUUID();

        when(organizationServiceClient.findOrganizationsForUser(umsUserId))
                .thenReturn(List.of(new OrganizationSummary(UUID.randomUUID())));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> organizationAccessService.assertUserBelongsToOrganization(organizationId, umsUserId));

        assertEquals(400, exception.getStatusCode().value());
    }
}
