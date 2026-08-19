package com.ums.authorization.service.seeder;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ums.authorization.entity.Permission;
import com.ums.authorization.entity.Resource;
import com.ums.authorization.repository.PermissionRepository;
import com.ums.authorization.repository.ResourceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionSeeder {

    private final ResourceRepository resourceRepository;
    private final PermissionRepository permissionRepository;

    public void seed() {
        seedActions("USER", "READ", "WRITE", "CREATE", "UPDATE", "DELETE");
        seedActions("ROLE", "READ", "WRITE", "CREATE", "UPDATE", "ASSIGN");
        seedActions("ORGANIZATION", "READ", "WRITE", "CREATE", "UPDATE", "DELETE");
        seedActions("AUDIT", "READ");
        seedActions("DASHBOARD", "READ");
        seedActions("NOTIFICATION_LOG", "READ");
        seedActions("NOTIFICATION_TEMPLATE", "READ", "WRITE");
        seedActions("SESSION", "READ", "WRITE");
        seedActions("EMPLOYEE", "READ", "CREATE", "UPDATE");
        seedActions("DESIGNATION", "READ", "CREATE", "UPDATE");
        seedActions("ATTENDANCE", "READ", "CREATE", "UPDATE");
        seedActions("LEAVE", "READ", "REQUEST_CREATE", "APPROVE", "CANCEL");
        seedActions("PAYROLL", "VIEW", "PROCESS");
        seedActions("PROJECT", "CREATE", "UPDATE", "DELETE");
        seedActions("DEPARTMENT", "READ", "WRITE", "CREATE", "UPDATE", "DELETE");
    }

    private void seedActions(String resourceCode, String... actions) {
        List.of(actions).forEach(action -> createPermission(resourceCode, action));
    }

    private void createPermission(String resourceCode, String action) {
        Resource resource = resourceRepository.findByCodeIgnoreCase(resourceCode)
                .orElseThrow(() -> new IllegalStateException("Resource not found: " + resourceCode));
        String permissionCode = resourceCode + "_" + action;
        if (permissionRepository.findByCodeIgnoreCase(permissionCode).isEmpty()) {
            permissionRepository.save(Permission.builder()
                    .code(permissionCode)
                    .resource(resource)
                    .action(action)
                    .description(permissionCode)
                    .active(true)
                    .build());
        }
    }
}
