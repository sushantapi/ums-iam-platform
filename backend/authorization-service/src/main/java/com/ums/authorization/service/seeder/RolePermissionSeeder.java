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

		assign("HR_MANAGER", "USER_CREATE");
		assign("HR_MANAGER", "USER_UPDATE");
		assign("HR_MANAGER", "USER_DELETE");

		assign("PAYROLL_MANAGER", "PAYROLL_VIEW");
		assign("PAYROLL_MANAGER", "PAYROLL_PROCESS");
	}

	private void assign(String roleName, String permissionCode) {

		Role role = roleRepository.findByNameIgnoreCase(roleName)
				.orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

		Permission permission = permissionRepository.findByCodeIgnoreCase(permissionCode)
				.orElseThrow(() -> new RuntimeException("Permission not found: " + permissionCode));

		boolean exists = rolePermissionRepository.existsByRole_IdAndPermission_Id(role.getId(), permission.getId());

		if (!exists) {

			rolePermissionRepository.save(RolePermission.builder().role(role).permission(permission).build());
		}
	}

	private void assignAllToSuperAdmin() {

		Role superAdmin = roleRepository.findByNameIgnoreCase("SUPER_ADMIN")
				.orElseThrow(() -> new RuntimeException("SUPER_ADMIN role not found"));

		List<Permission> permissions = permissionRepository.findAll();

		for (Permission permission : permissions) {

			boolean exists = rolePermissionRepository.existsByRole_IdAndPermission_Id(superAdmin.getId(),
					permission.getId());

			if (!exists) {

				rolePermissionRepository.save(RolePermission.builder().role(superAdmin).permission(permission).build());
			}
		}
	}
}