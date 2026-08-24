package com.ums.org.controller;

import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ums.org.dto.OrganizationLogoAssetResponse;
import com.ums.org.dto.OrganizationLogoDocument;
import com.ums.org.service.OrganizationLogoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/profile/logo")
@RequiredArgsConstructor
public class OrganizationLogoController {

    private final OrganizationLogoService organizationLogoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OrganizationLogoAssetResponse> upload(
            @PathVariable UUID organizationId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationLogoService.upload(
                        organizationId,
                        file,
                        authenticatedUserId(authentication),
                        isSuperAdmin(authentication)));
    }

    @GetMapping
    public ResponseEntity<byte[]> current(
            @PathVariable UUID organizationId,
            Authentication authentication) {
        return binaryResponse(organizationLogoService.getCurrent(
                organizationId,
                authenticatedUserId(authentication),
                isSuperAdmin(authentication)));
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<byte[]> version(
            @PathVariable UUID organizationId,
            @PathVariable UUID assetId,
            Authentication authentication) {
        return binaryResponse(organizationLogoService.getVersion(
                organizationId,
                assetId,
                authenticatedUserId(authentication),
                isSuperAdmin(authentication)));
    }

    private ResponseEntity<byte[]> binaryResponse(OrganizationLogoDocument document) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(document.contentType()));
        headers.setContentLength(document.content().length);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(document.fileName())
                .build());
        return new ResponseEntity<>(document.content(), headers, HttpStatus.OK);
    }

    private UUID authenticatedUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private boolean isSuperAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }
}
