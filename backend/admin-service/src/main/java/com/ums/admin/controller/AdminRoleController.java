package com.ums.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.admin.dto.request.AssignRoleRequest;
import com.ums.admin.service.AdminRoleService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @PostMapping("/assign")
    public ResponseEntity<String> assignRole(
            @Valid @RequestBody AssignRoleRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(adminRoleService.assignRole(request));
    }
}