package com.ums.hrms.employee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.employee.dto.CreateDesignationRequest;
import com.ums.hrms.employee.entity.Designation;
import com.ums.hrms.employee.entity.MasterDataStatus;
import com.ums.hrms.employee.repository.DesignationRepository;

@ExtendWith(MockitoExtension.class)
class DesignationServiceTests {

    @Mock
    private DesignationRepository designationRepository;

    @Mock
    private OrganizationAccessService organizationAccessService;

    @InjectMocks
    private DesignationService designationService;

    @Test
    void createPersistsTenantScopedDesignation() {
        UUID organizationId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID designationId = UUID.randomUUID();

        when(designationRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, "SSE"))
                .thenReturn(false);
        when(designationRepository.save(any(Designation.class))).thenAnswer(invocation -> {
            Designation designation = invocation.getArgument(0);
            designation.setId(designationId);
            return designation;
        });

        var response = designationService.create(
                new CreateDesignationRequest(organizationId, " sse ", " Senior Software Engineer ", null),
                actorUserId,
                false);

        verify(organizationAccessService).assertCanAccess(organizationId, actorUserId, false);
        assertEquals(designationId, response.id());
        assertEquals(organizationId, response.organizationId());
        assertEquals("SSE", response.code());
        assertEquals("Senior Software Engineer", response.name());
        assertEquals(MasterDataStatus.ACTIVE, response.status());
    }

    @Test
    void getNeverReadsDesignationOutsideRequestedOrganization() {
        UUID designationId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        when(designationRepository.findByIdAndOrganizationId(designationId, organizationId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> designationService.get(designationId, organizationId, actorUserId, false));

        assertEquals(404, exception.getStatusCode().value());
        verify(organizationAccessService).assertCanAccess(organizationId, actorUserId, false);
    }
}
