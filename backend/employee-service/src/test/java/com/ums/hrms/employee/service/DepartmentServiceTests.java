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

import com.ums.hrms.employee.dto.CreateDepartmentRequest;
import com.ums.hrms.employee.entity.Department;
import com.ums.hrms.employee.entity.MasterDataStatus;
import com.ums.hrms.employee.repository.DepartmentRepository;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTests {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private OrganizationAccessService organizationAccessService;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    void createPersistsTenantScopedDepartment() {
        UUID organizationId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        when(departmentRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, "ENG"))
                .thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department department = invocation.getArgument(0);
            department.setId(departmentId);
            return department;
        });

        var response = departmentService.create(
                new CreateDepartmentRequest(organizationId, " eng ", " Engineering ", " Product engineering "),
                actorUserId,
                false);

        verify(organizationAccessService).assertCanAccess(organizationId, actorUserId, false);
        assertEquals(departmentId, response.id());
        assertEquals(organizationId, response.organizationId());
        assertEquals("ENG", response.code());
        assertEquals("Engineering", response.name());
        assertEquals(MasterDataStatus.ACTIVE, response.status());
    }

    @Test
    void getNeverReadsDepartmentOutsideRequestedOrganization() {
        UUID departmentId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        when(departmentRepository.findByIdAndOrganizationId(departmentId, organizationId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> departmentService.get(departmentId, organizationId, actorUserId, false));

        assertEquals(404, exception.getStatusCode().value());
        verify(organizationAccessService).assertCanAccess(organizationId, actorUserId, false);
    }
}
