package com.ums.org.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.org.dto.OrganizationProfileResponse;
import com.ums.org.dto.UpdateOrganizationProfileRequest;
import com.ums.org.service.OrganizationProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/profile")
@RequiredArgsConstructor
public class OrganizationProfileController {

    private final OrganizationProfileService organizationProfileService;

    @GetMapping
    public ResponseEntity<OrganizationProfileResponse> get(
            @PathVariable UUID organizationId,
            Authentication authentication) {
        return ResponseEntity.ok(organizationProfileService.get(
                organizationId,
                authenticatedUserId(authentication),
                isSuperAdmin(authentication)));
    }

    @PutMapping
    public ResponseEntity<OrganizationProfileResponse> update(
            @PathVariable UUID organizationId,
            @Valid @RequestBody UpdateOrganizationProfileRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(organizationProfileService.update(
                organizationId,
                request,
                authenticatedUserId(authentication),
                isSuperAdmin(authentication)));
    }

    private UUID authenticatedUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private boolean isSuperAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }
}
