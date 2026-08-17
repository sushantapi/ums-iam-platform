package com.ums.hrms.employee.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.employee.dto.CreateDesignationRequest;
import com.ums.hrms.employee.dto.DesignationPageResponse;
import com.ums.hrms.employee.dto.DesignationResponse;
import com.ums.hrms.employee.dto.UpdateDesignationRequest;
import com.ums.hrms.employee.entity.Designation;
import com.ums.hrms.employee.entity.MasterDataStatus;
import com.ums.hrms.employee.repository.DesignationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DesignationService {

    private final DesignationRepository designationRepository;
    private final OrganizationAccessService organizationAccessService;

    public DesignationResponse create(CreateDesignationRequest request, UUID actorUserId, boolean superAdmin) {
        organizationAccessService.assertCanAccess(request.organizationId(), actorUserId, superAdmin);

        String code = normalizeCode(request.code());
        if (designationRepository.existsByOrganizationIdAndCodeIgnoreCase(request.organizationId(), code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Designation code already exists in organization");
        }

        Designation designation = Designation.builder()
                .organizationId(request.organizationId())
                .code(code)
                .name(normalizeName(request.name()))
                .description(normalizeDescription(request.description()))
                .status(MasterDataStatus.ACTIVE)
                .build();

        return toResponse(designationRepository.save(designation));
    }

    @Transactional(readOnly = true)
    public DesignationPageResponse list(
            UUID organizationId,
            int page,
            int size,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);
        validatePage(page, size);

        var designations = designationRepository.findAllByOrganizationId(
                organizationId,
                PageRequest.of(page, size, Sort.by("name").ascending()));

        return new DesignationPageResponse(
                designations.getContent().stream().map(this::toResponse).toList(),
                designations.getNumber(),
                designations.getSize(),
                designations.getTotalElements(),
                designations.getTotalPages());
    }

    @Transactional(readOnly = true)
    public DesignationResponse get(UUID designationId, UUID organizationId, UUID actorUserId, boolean superAdmin) {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);
        return toResponse(findScoped(designationId, organizationId));
    }

    public DesignationResponse update(
            UUID designationId,
            UpdateDesignationRequest request,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(request.organizationId(), actorUserId, superAdmin);
        Designation designation = findScoped(designationId, request.organizationId());

        String code = normalizeCode(request.code());
        if (!designation.getCode().equalsIgnoreCase(code)
                && designationRepository.existsByOrganizationIdAndCodeIgnoreCase(request.organizationId(), code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Designation code already exists in organization");
        }

        designation.setCode(code);
        designation.setName(normalizeName(request.name()));
        designation.setDescription(normalizeDescription(request.description()));
        designation.setStatus(request.status());

        return toResponse(designationRepository.save(designation));
    }

    private Designation findScoped(UUID designationId, UUID organizationId) {
        return designationRepository.findByIdAndOrganizationId(designationId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designation not found"));
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

    private DesignationResponse toResponse(Designation designation) {
        return new DesignationResponse(
                designation.getId(),
                designation.getOrganizationId(),
                designation.getCode(),
                designation.getName(),
                designation.getDescription(),
                designation.getStatus(),
                designation.getCreatedAt(),
                designation.getUpdatedAt());
    }
}
