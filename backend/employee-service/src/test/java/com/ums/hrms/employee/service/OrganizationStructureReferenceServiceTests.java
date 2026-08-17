package com.ums.hrms.employee.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.employee.entity.Department;
import com.ums.hrms.employee.entity.Designation;
import com.ums.hrms.employee.entity.MasterDataStatus;
import com.ums.hrms.employee.repository.DepartmentRepository;
import com.ums.hrms.employee.repository.DesignationRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationStructureReferenceServiceTests {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DesignationRepository designationRepository;

    @InjectMocks
    private OrganizationStructureReferenceService referenceService;

    @Test
    void acceptsActiveReferencesFromSameOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID designationId = UUID.randomUUID();

        when(departmentRepository.findByIdAndOrganizationId(departmentId, organizationId))
                .thenReturn(Optional.of(Department.builder()
                        .id(departmentId)
                        .organizationId(organizationId)
                        .status(MasterDataStatus.ACTIVE)
                        .build()));
        when(designationRepository.findByIdAndOrganizationId(designationId, organizationId))
                .thenReturn(Optional.of(Designation.builder()
                        .id(designationId)
                        .organizationId(organizationId)
                        .status(MasterDataStatus.ACTIVE)
                        .build()));

        assertDoesNotThrow(() -> referenceService.validateActiveReferences(
                organizationId,
                departmentId,
                designationId));
    }

    @Test
    void rejectsDepartmentOutsideOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        when(departmentRepository.findByIdAndOrganizationId(departmentId, organizationId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> referenceService.validateActiveReferences(organizationId, departmentId, null));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void rejectsInactiveDesignation() {
        UUID organizationId = UUID.randomUUID();
        UUID designationId = UUID.randomUUID();

        when(designationRepository.findByIdAndOrganizationId(designationId, organizationId))
                .thenReturn(Optional.of(Designation.builder()
                        .id(designationId)
                        .organizationId(organizationId)
                        .status(MasterDataStatus.INACTIVE)
                        .build()));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> referenceService.validateActiveReferences(organizationId, null, designationId));

        assertEquals(400, exception.getStatusCode().value());
    }
}
