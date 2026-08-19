package com.ums.hrms.leave.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.leave.dto.CreateLeaveRequest;
import com.ums.hrms.leave.dto.LeavePageResponse;
import com.ums.hrms.leave.dto.LeaveResponse;
import com.ums.hrms.leave.entity.LeaveRequest;
import com.ums.hrms.leave.entity.LeaveStatus;
import com.ums.hrms.leave.repository.LeaveRequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final OrganizationAccessService organizationAccessService;
    private final LeaveTenantValidationService employeeValidationService;

    public LeaveResponse create(CreateLeaveRequest request, UUID actorUserId, boolean superAdmin) {
        organizationAccessService.assertCanAccess(request.organizationId(), actorUserId, superAdmin);
        employeeValidationService.validateEmployeeBelongsToOrganization(
                request.employeeId(), request.organizationId());
        validateDateRange(request.startDate(), request.endDate());

        if (leaveRequestRepository.existsOverlappingActiveRequest(
                request.organizationId(),
                request.employeeId(),
                request.startDate(),
                request.endDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Overlapping leave request exists");
        }

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setOrganizationId(request.organizationId());
        leaveRequest.setEmployeeId(request.employeeId());
        leaveRequest.setLeaveType(request.leaveType());
        leaveRequest.setStartDate(request.startDate());
        leaveRequest.setEndDate(request.endDate());
        leaveRequest.setReason(normalizeReason(request.reason()));
        leaveRequest.setStatus(LeaveStatus.PENDING);
        leaveRequest.setRequestedBy(actorUserId);

        return toResponse(leaveRequestRepository.save(leaveRequest));
    }

    @Transactional(readOnly = true)
    public LeavePageResponse list(
            UUID organizationId,
            int page,
            int size,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);
        validatePage(page, size);

        var leaves = leaveRequestRepository.findAllByOrganizationId(
                organizationId,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Order.desc("startDate"), Sort.Order.desc("createdAt"))));

        return new LeavePageResponse(
                leaves.getContent().stream().map(this::toResponse).toList(),
                leaves.getNumber(),
                leaves.getSize(),
                leaves.getTotalElements(),
                leaves.getTotalPages());
    }

    @Transactional(readOnly = true)
    public LeaveResponse get(
            UUID id,
            UUID organizationId,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);
        return leaveRequestRepository.findByIdAndOrganizationId(id, organizationId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Leave request not found"));
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or size");
        }
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String normalized = reason.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private LeaveResponse toResponse(LeaveRequest leaveRequest) {
        return new LeaveResponse(
                leaveRequest.getId(),
                leaveRequest.getOrganizationId(),
                leaveRequest.getEmployeeId(),
                leaveRequest.getLeaveType(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                leaveRequest.getReason(),
                leaveRequest.getStatus(),
                leaveRequest.getRequestedBy(),
                leaveRequest.getDecidedBy(),
                leaveRequest.getDecidedAt(),
                leaveRequest.getDecisionComment(),
                leaveRequest.getCreatedAt(),
                leaveRequest.getUpdatedAt());
    }
}
