package com.ums.authorization.service.seeder;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ums.authorization.entity.Permission;
import com.ums.authorization.entity.Role;
import com.ums.authorization.entity.RolePermission;
import com.ums.authorization.repository.PermissionRepository;
import com.ums.authorization.repository.RolePermissionRepository;
import com.ums.authorization.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RolePermissionSeeder {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public void seed() {
        assignAllToSuperAdmin();

        assignMany("USER_ADMIN", "USER_READ", "USER_WRITE", "USER_CREATE", "USER_UPDATE", "USER_DELETE");
        assignMany("AUTH_ADMIN", "ROLE_READ", "ROLE_WRITE", "ROLE_CREATE", "ROLE_UPDATE", "ROLE_ASSIGN",
                "SESSION_READ", "SESSION_WRITE");
        assignMany("AUDIT_ADMIN", "AUDIT_READ");
        assignMany("SECURITY", "AUDIT_READ", "SESSION_READ");
        assignMany("COMPLIANCE", "AUDIT_READ");
        assignMany("SUPPORT", "USER_READ", "ROLE_READ", "DASHBOARD_READ", "SESSION_READ",
                "NOTIFICATION_LOG_READ");
        assignMany("NOTIFICATION_ADMIN", "NOTIFICATION_LOG_READ", "NOTIFICATION_TEMPLATE_READ",
                "NOTIFICATION_TEMPLATE_WRITE");
        assignMany("ORG_ADMIN", "ORGANIZATION_READ", "ORGANIZATION_WRITE", "USER_READ", "ROLE_READ");
        assignMany("HR_MANAGER", "USER_READ", "USER_WRITE", "USER_CREATE", "USER_UPDATE",
                "ORGANIZATION_READ", "DEPARTMENT_READ", "DEPARTMENT_WRITE", "DEPARTMENT_CREATE",
                "DEPARTMENT_UPDATE", "DESIGNATION_READ", "DESIGNATION_CREATE", "DESIGNATION_UPDATE",
                "EMPLOYEE_READ", "EMPLOYEE_CREATE", "EMPLOYEE_UPDATE",
                "ATTENDANCE_READ", "ATTENDANCE_CREATE", "ATTENDANCE_UPDATE",
                "LEAVE_READ", "LEAVE_REQUEST_CREATE", "LEAVE_APPROVE", "LEAVE_CANCEL");
        assignMany("PAYROLL_MANAGER", "PAYROLL_VIEW", "PAYROLL_PROCESS");
    }

    private void assignMany(String roleName, String... permissionCodes) {
        List.of(permissionCodes).forEach(permissionCode -> assign(roleName, permissionCode));
    }

    private void assign(String roleName, String permissionCode) {
        Role role = roleRepository.findByNameIgnoreCase(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + roleName));
        Permission permission = permissionRepository.findByCodeIgnoreCase(permissionCode)
                .orElseThrow(() -> new IllegalStateException("Permission not found: " + permissionCode));
        if (!rolePermissionRepository.existsByRole_IdAndPermission_Id(role.getId(), permission.getId())) {
            rolePermissionRepository.save(RolePermission.builder().role(role).permission(permission).build());
        }
    }

    private void assignAllToSuperAdmin() {
        Role superAdmin = roleRepository.findByNameIgnoreCase("SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role not found"));
        for (Permission permission : permissionRepository.findAll()) {
            if (!rolePermissionRepository.existsByRole_IdAndPermission_Id(superAdmin.getId(), permission.getId())) {
                rolePermissionRepository.save(RolePermission.builder().role(superAdmin).permission(permission).build());
            }
        }
    }
}
