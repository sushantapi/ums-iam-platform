package com.ums.hrms.employee.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.employee.dto.CreateDepartmentRequest;
import com.ums.hrms.employee.dto.DepartmentPageResponse;
import com.ums.hrms.employee.dto.DepartmentResponse;
import com.ums.hrms.employee.dto.UpdateDepartmentRequest;
import com.ums.hrms.employee.entity.Department;
import com.ums.hrms.employee.entity.MasterDataStatus;
import com.ums.hrms.employee.repository.DepartmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final OrganizationAccessService organizationAccessService;

    public DepartmentResponse create(CreateDepartmentRequest request, UUID actorUserId, boolean superAdmin) {
        organizationAccessService.assertCanAccess(request.organizationId(), actorUserId, superAdmin);

        String code = normalizeCode(request.code());
        if (departmentRepository.existsByOrganizationIdAndCodeIgnoreCase(request.organizationId(), code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department code already exists in organization");
        }

        Department department = Department.builder()
                .organizationId(request.organizationId())
                .code(code)
                .name(normalizeName(request.name()))
                .description(normalizeDescription(request.description()))
                .status(MasterDataStatus.ACTIVE)
                .build();

        return toResponse(departmentRepository.save(department));
    }

    @Transactional(readOnly = true)
    public DepartmentPageResponse list(
            UUID organizationId,
            int page,
            int size,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);
        validatePage(page, size);

        var departments = departmentRepository.findAllByOrganizationId(
                organizationId,
                PageRequest.of(page, size, Sort.by("name").ascending()));

        return new DepartmentPageResponse(
                departments.getContent().stream().map(this::toResponse).toList(),
                departments.getNumber(),
                departments.getSize(),
                departments.getTotalElements(),
                departments.getTotalPages());
    }

    @Transactional(readOnly = true)
    public DepartmentResponse get(UUID departmentId, UUID organizationId, UUID actorUserId, boolean superAdmin) {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);
        return toResponse(findScoped(departmentId, organizationId));
    }

    public DepartmentResponse update(
            UUID departmentId,
            UpdateDepartmentRequest request,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(request.organizationId(), actorUserId, superAdmin);
        Department department = findScoped(departmentId, request.organizationId());

        String code = normalizeCode(request.code());
        if (!department.getCode().equalsIgnoreCase(code)
                && departmentRepository.existsByOrganizationIdAndCodeIgnoreCase(request.organizationId(), code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department code already exists in organization");
        }

        department.setCode(code);
        department.setName(normalizeName(request.name()));
        department.setDescription(normalizeDescription(request.description()));
        department.setStatus(request.status());

        return toResponse(departmentRepository.save(department));
    }

    private Department findScoped(UUID departmentId, UUID organizationId) {
        return departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or size");
        }
    }

    private DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getOrganizationId(),
                department.getCode(),
                department.getName(),
                department.getDescription(),
                department.getStatus(),
                department.getCreatedAt(),
                department.getUpdatedAt());
    }
}
